package com.pickpackship.order;

import com.pickpackship.order.api.dto.CreateOrderRequest;
import com.pickpackship.order.api.dto.OrderResponse;
import com.pickpackship.order.domain.Address;
import com.pickpackship.order.domain.Order;
import com.pickpackship.order.domain.OrderStatus;
import com.pickpackship.order.domain.OutboxEvent;
import com.pickpackship.order.domain.Party;
import com.pickpackship.order.domain.Seller;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    void creatingOrderPersistsItAndWritesCreatedOutboxEvent() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "12345678900"));
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateOrderRequest request = validRequest(seller.getSellerId(), 1001L, Map.of("SKU-1", 2));
        HttpEntity<CreateOrderRequest> httpRequest = authenticatedJsonRequest(adminCookie, request);

        ResponseEntity<OrderResponse> response =
                restTemplate.postForEntity("/order", httpRequest, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrderResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.customerName()).isEqualTo("John Doe");
        assertThat(body.orderNumber()).isEqualTo(1001L);
        assertThat(body.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(body.items()).isEqualTo(Map.of("SKU-1", 2));

        Order saved = orderRepository.findById(body.orderId()).orElseThrow();
        assertThat(saved.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        OutboxEvent event = outboxEvents.get(0);
        assertThat(event.getEventType()).isEqualTo("order.created");
        assertThat(event.isPublished()).isFalse();
        assertThat(event.getPayload()).contains(body.orderId().toString());
        assertThat(event.getPayload()).contains(workspaceId.toString());
    }

    @Test
    void creatingOrderWithNonExistentSellerReturnsBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateOrderRequest request = validRequest(UUID.randomUUID(), 1002L, Map.of("SKU-1", 1));
        HttpEntity<CreateOrderRequest> httpRequest = authenticatedJsonRequest(adminCookie, request);

        ResponseEntity<String> response = restTemplate.postForEntity("/order", httpRequest, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.CONFLICT);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void creatingOrderWithoutItemsIsRejected() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "12345678900"));
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateOrderRequest request = validRequest(seller.getSellerId(), 1003L, Map.of());
        HttpEntity<CreateOrderRequest> httpRequest = authenticatedJsonRequest(adminCookie, request);

        ResponseEntity<String> response = restTemplate.postForEntity("/order", httpRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void creatingOrderWithoutAuthenticationIsRejected() {
        CreateOrderRequest request = validRequest(UUID.randomUUID(), 1004L, Map.of("SKU-1", 1));

        ResponseEntity<String> response = restTemplate.postForEntity("/order", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nonAdminCannotCreateOrder() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "12345678900"));
        String pickerCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "PICKER");

        CreateOrderRequest request = validRequest(seller.getSellerId(), 1005L, Map.of("SKU-1", 1));
        HttpEntity<CreateOrderRequest> httpRequest = authenticatedJsonRequest(pickerCookie, request);

        ResponseEntity<String> response = restTemplate.postForEntity("/order", httpRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    static CreateOrderRequest validRequest(UUID seller, Long orderNumber, Map<String, Integer> items) {
        Address address = new Address(
                "Main St", "100", "Downtown", "Springfield", "SP", "01000-000");
        Party sender = new Party("Warehouse Co", "11122233344", address);
        Party recipient = new Party("John Doe", "99988877766", address);

        return new CreateOrderRequest("John Doe", seller, orderNumber, sender, recipient, items);
    }
}
