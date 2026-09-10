package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.domain.OutboxEvent;
import com.pickpackship.auth.domain.User;
import com.pickpackship.auth.outbox.OutboxPublisher;
import java.time.Duration;
import java.util.List;
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

class WorkspaceDeletedOutboxIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPIC = "workspace.deleted";

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Test
    void deletingWorkspaceWritesOutboxRowAndPollerPublishesToKafka() {
        SignUpRequest signUp = new SignUpRequest("Acme Logistics", "admin.acme", "adminPassword1");
        ResponseEntity<Void> signUpResponse = restTemplate.postForEntity("/auth/signup", signUp, Void.class);
        String adminCookie = extractAccessTokenCookie(signUpResponse);

        User admin = userRepository.findByUserName("admin.acme").orElseThrow();
        UUID workspaceId = admin.getWorkspaceId();

        HttpEntity<Void> deleteRequest = authenticatedRequest(adminCookie);
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/auth/workspaces/" + workspaceId, HttpMethod.DELETE, deleteRequest, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(userRepository.findByWorkspaceId(workspaceId)).isEmpty();
        assertThat(workspaceRepository.findById(workspaceId)).isEmpty();

        List<OutboxEvent> pending = outboxEventRepository.findAll();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo(TOPIC);
        assertThat(pending.get(0).isPublished()).isFalse();
        assertThat(pending.get(0).getPayload()).contains(workspaceId.toString());

        // Don't wait for the real 24h @Scheduled tick: trigger the poller directly.
        outboxPublisher.publishPending();

        Optional<OutboxEvent> afterPublish = outboxEventRepository.findById(pending.get(0).getId());
        assertThat(afterPublish).isPresent();
        assertThat(afterPublish.get().isPublished()).isTrue();

        try (KafkaConsumer<String, String> consumer = kafkaConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.value()).contains(workspaceId.toString());
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
