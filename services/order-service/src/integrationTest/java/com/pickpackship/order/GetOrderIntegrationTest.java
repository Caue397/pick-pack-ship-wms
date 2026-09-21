package com.pickpackship.order;

import com.pickpackship.order.api.dto.OrderResponse;
import com.pickpackship.order.domain.Address;
import com.pickpackship.order.domain.Order;
import com.pickpackship.order.domain.Party;
import com.pickpackship.order.domain.Seller;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GetOrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    void callerCanFetchOrderFromOwnWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        Order order = orderRepository.save(newOrder(workspaceId));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/order/" + order.getOrderId(), HttpMethod.GET, authenticatedRequest(cookie), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().orderId()).isEqualTo(order.getOrderId());
    }

    @Test
    void callerCannotFetchOrderFromAnotherWorkspace() {
        UUID ownerWorkspace = UUID.randomUUID();
        Order order = orderRepository.save(newOrder(ownerWorkspace));
        String otherWorkspaceCookie = cookiePairFor(UUID.randomUUID(), "OP-002", UUID.randomUUID(), "ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/order/" + order.getOrderId(), HttpMethod.GET,
                authenticatedRequest(otherWorkspaceCookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void fetchingNonExistentOrderReturnsNotFound() {
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", UUID.randomUUID(), "ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/order/" + UUID.randomUUID(), HttpMethod.GET, authenticatedRequest(cookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Order newOrder(UUID workspaceId) {
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", UUID.randomUUID().toString()));
        Address address = new Address("Main St", "100", "Downtown", "Springfield", "SP", "01000-000");
        Party party = new Party("John Doe", "99988877766", address);
        return Order.build(workspaceId, "John Doe", 2001L, seller.getSellerId(), party, party, Map.of("SKU-1", 1));
    }
}
