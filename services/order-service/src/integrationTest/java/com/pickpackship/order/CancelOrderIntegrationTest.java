package com.pickpackship.order;

import com.pickpackship.order.api.dto.CancelOrderRequest;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CancelOrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    void cancellingOwnOrderUpdatesStatusAndWritesCancelledOutboxEvent() {
        UUID workspaceId = UUID.randomUUID();
        Order order = newOrder(workspaceId, 5001L);
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CancelOrderRequest request = new CancelOrderRequest("Customer requested cancellation");
        HttpEntity<CancelOrderRequest> httpRequest = authenticatedJsonRequest(cookie, request);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/order/cancel/" + order.getOrderId(), HttpMethod.POST, httpRequest, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.getCancellationReason()).isEqualTo("Customer requested cancellation");

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("order.cancelled");
        assertThat(outboxEvents.get(0).getPayload()).contains(order.getOrderId().toString());
    }

    @Test
    void cancellingOrderFromAnotherWorkspaceIsForbidden() {
        UUID ownerWorkspace = UUID.randomUUID();
        Order order = newOrder(ownerWorkspace, 5002L);
        String otherWorkspaceCookie = cookiePairFor(UUID.randomUUID(), "OP-002", UUID.randomUUID(), "ADMIN");

        CancelOrderRequest request = new CancelOrderRequest("Not my order");
        HttpEntity<CancelOrderRequest> httpRequest = authenticatedJsonRequest(otherWorkspaceCookie, request);

        ResponseEntity<String> response = restTemplate.exchange(
                "/order/cancel/" + order.getOrderId(), HttpMethod.POST, httpRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Order unchanged = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void nonAdminCannotCancelOrder() {
        UUID workspaceId = UUID.randomUUID();
        Order order = newOrder(workspaceId, 5003L);
        String pickerCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "PICKER");

        CancelOrderRequest request = new CancelOrderRequest("Trying to cancel without permission");
        HttpEntity<CancelOrderRequest> httpRequest = authenticatedJsonRequest(pickerCookie, request);

        ResponseEntity<String> response = restTemplate.exchange(
                "/order/cancel/" + order.getOrderId(), HttpMethod.POST, httpRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Order unchanged = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void cancellingNonExistentOrderReturnsNotFound() {
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", UUID.randomUUID(), "ADMIN");
        CancelOrderRequest request = new CancelOrderRequest("Doesn't matter");
        HttpEntity<CancelOrderRequest> httpRequest = authenticatedJsonRequest(cookie, request);

        ResponseEntity<String> response = restTemplate.exchange(
                "/order/cancel/" + UUID.randomUUID(), HttpMethod.POST, httpRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Order newOrder(UUID workspaceId, long orderNumber) {
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", UUID.randomUUID().toString()));
        Address address = new Address("Main St", "100", "Downtown", "Springfield", "SP", "01000-000");
        Party party = new Party("John Doe", "99988877766", address);
        Order order = Order.build(
                workspaceId, "John Doe", orderNumber, seller.getSellerId(), party, party, Map.of("SKU-1", 1));
        return orderRepository.save(order);
    }
}
