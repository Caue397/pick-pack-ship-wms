package com.pickpackship.order;

import com.pickpackship.order.api.dto.CancelOrderRequest;
import com.pickpackship.order.api.dto.CreateOrderRequest;
import com.pickpackship.order.api.dto.OrderResponse;
import com.pickpackship.order.domain.OutboxEvent;
import com.pickpackship.order.domain.Seller;
import com.pickpackship.order.outbox.OutboxPublisher;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the outbox pattern end-to-end: a domain write leaves an unpublished row
 * in the outbox table within the same transaction, and the scheduled poller (triggered
 * directly here, instead of waiting on its real fixedDelay) later marks it published
 * and hands it to Kafka.
 */
class OutboxPublisherIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Test
    void orderCreatedEventIsWrittenToOutboxAndPublishedToKafka() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", UUID.randomUUID().toString()));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateOrderRequest request =
                CreateOrderIntegrationTest.validRequest(seller.getSellerId(), 6001L, Map.of("SKU-1", 3));
        HttpEntity<CreateOrderRequest> httpRequest = authenticatedJsonRequest(cookie, request);

        ResponseEntity<OrderResponse> createResponse =
                restTemplate.postForEntity("/order", httpRequest, OrderResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID orderId = createResponse.getBody().orderId();

        assertOutboxRowIsPublishedAndDeliveredTo("order.created", orderId);
    }

    @Test
    void orderCancelledEventIsWrittenToOutboxAndPublishedToKafka() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", UUID.randomUUID().toString()));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateOrderRequest createRequest =
                CreateOrderIntegrationTest.validRequest(seller.getSellerId(), 6002L, Map.of("SKU-1", 1));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "/order", authenticatedJsonRequest(cookie, createRequest), OrderResponse.class);
        UUID orderId = createResponse.getBody().orderId();

        // Publish the order.created event first so only order.cancelled remains pending.
        outboxPublisher.publishPending();

        CancelOrderRequest cancelRequest = new CancelOrderRequest("No longer needed");
        ResponseEntity<Void> cancelResponse = restTemplate.exchange(
                "/order/cancel/" + orderId, HttpMethod.POST,
                authenticatedJsonRequest(cookie, cancelRequest), Void.class);
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertOutboxRowIsPublishedAndDeliveredTo("order.cancelled", orderId);
    }

    private void assertOutboxRowIsPublishedAndDeliveredTo(String eventType, UUID orderId) {
        List<OutboxEvent> pending = outboxEventRepository.findAll().stream()
                .filter(e -> e.getEventType().equals(eventType))
                .toList();
        assertThat(pending).hasSize(1);
        OutboxEvent event = pending.get(0);
        assertThat(event.isPublished()).isFalse();

        outboxPublisher.publishPending();

        Optional<OutboxEvent> afterPublish = outboxEventRepository.findById(event.getId());
        assertThat(afterPublish).isPresent();
        assertThat(afterPublish.get().isPublished()).isTrue();

        try (KafkaConsumer<String, String> consumer = kafkaConsumer()) {
            consumer.subscribe(List.of(eventType));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.value()).contains(orderId.toString());
        }
    }

    private KafkaConsumer<String, String> kafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
