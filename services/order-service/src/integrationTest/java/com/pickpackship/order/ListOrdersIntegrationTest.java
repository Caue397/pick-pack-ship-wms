package com.pickpackship.order;

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

class ListOrdersIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listOnlyReturnsOrdersFromCallersWorkspace() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        newOrder(workspaceA, 3001L);
        newOrder(workspaceA, 3002L);
        newOrder(workspaceB, 3003L);

        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceA, "ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/order", HttpMethod.GET, authenticatedRequest(cookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("3001").contains("3002");
        assertThat(response.getBody()).doesNotContain("3003");
    }

    @Test
    void listCanFilterByOrderNumber() {
        UUID workspaceId = UUID.randomUUID();
        newOrder(workspaceId, 4001L);
        newOrder(workspaceId, 4002L);

        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/order?orderNumber=4001", HttpMethod.GET, authenticatedRequest(cookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("4001");
        assertThat(response.getBody()).doesNotContain("4002");
    }

    @Test
    void listWithoutAuthenticationIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/order", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
