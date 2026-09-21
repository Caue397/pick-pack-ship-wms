package com.pickpackship.order;

import com.pickpackship.order.api.dto.CreateSellerRequest;
import com.pickpackship.order.api.dto.SellerResponse;
import com.pickpackship.order.api.dto.UpdateSellerRequest;
import com.pickpackship.order.domain.Address;
import com.pickpackship.order.domain.Order;
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

class SellerIntegrationTest extends AbstractIntegrationTest {

    /**
     * The paginated envelope Spring Data returns for GET /order/seller. Only the
     * fields the assertions care about; unknown ones are ignored by Jackson.
     */
    private record SellerPage(
            List<SellerResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {}

    @Test
    void adminCanCreateSeller() {
        UUID workspaceId = UUID.randomUUID();
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateSellerRequest request = new CreateSellerRequest("Acme Sellers", "12345678900");
        ResponseEntity<SellerResponse> response = restTemplate.postForEntity(
                "/order/seller", authenticatedJsonRequest(adminCookie, request), SellerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("Acme Sellers");
        assertThat(response.getBody().workspaceId()).isEqualTo(workspaceId);
    }

    @Test
    void nonAdminCannotCreateSeller() {
        String pickerCookie = cookiePairFor(UUID.randomUUID(), "OP-001", UUID.randomUUID(), "PICKER");

        CreateSellerRequest request = new CreateSellerRequest("Acme Sellers", "12345678900");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/order/seller", authenticatedJsonRequest(pickerCookie, request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void creatingSellerWithDuplicateDocumentIsRejected() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Existing Seller", "12345678900"));
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        CreateSellerRequest request = new CreateSellerRequest("Acme Sellers", "12345678900");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/order/seller", authenticatedJsonRequest(adminCookie, request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void listSellersOnlyReturnsCallersWorkspace() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceA, "Seller A", "11111111111"));
        sellerRepository.save(Seller.build(workspaceB, "Seller B", "22222222222"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceA, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller", HttpMethod.GET, authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).extracting(SellerResponse::name).containsExactly("Seller A");
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    @Test
    void listSellersCanFilterByNameIgnoringCase() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Globex Trading", "22222222222"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?name=acme", HttpMethod.GET, authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).extracting(SellerResponse::name).containsExactly("Acme Sellers");
    }

    @Test
    void listSellersCanFilterByDocument() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Globex Trading", "22222222222"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?document=22222222222", HttpMethod.GET,
                authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).extracting(SellerResponse::document).containsExactly("22222222222");
    }

    @Test
    void listSellersFiltersAreCombined() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Acme Logistics", "22222222222"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?name=acme&document=11111111111", HttpMethod.GET,
                authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).extracting(SellerResponse::name).containsExactly("Acme Sellers");
    }

    @Test
    void listSellersFilterNeverLeaksAnotherWorkspace() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceB, "Acme Sellers", "11111111111"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceA, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?name=acme", HttpMethod.GET, authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).isEmpty();
        assertThat(response.getBody().totalElements()).isZero();
    }

    @Test
    void listSellersIsPaginated() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Globex Trading", "22222222222"));
        sellerRepository.save(Seller.build(workspaceId, "Zenith Supply", "33333333333"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?page=0&size=2", HttpMethod.GET, authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSize(2);
        assertThat(response.getBody().totalElements()).isEqualTo(3);
        assertThat(response.getBody().totalPages()).isEqualTo(2);
        assertThat(response.getBody().number()).isZero();
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    @Test
    void listSellersReturnsRequestedPage() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Globex Trading", "22222222222"));
        sellerRepository.save(Seller.build(workspaceId, "Zenith Supply", "33333333333"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?page=1&size=2&sort=name,asc", HttpMethod.GET,
                authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().number()).isEqualTo(1);
        assertThat(response.getBody().content()).extracting(SellerResponse::name).containsExactly("Zenith Supply");
    }

    @Test
    void listSellersCanBeSortedByName() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Globex Trading", "22222222222"));
        sellerRepository.save(Seller.build(workspaceId, "Zenith Supply", "33333333333"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?sort=name,desc", HttpMethod.GET, authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content())
                .extracting(SellerResponse::name)
                .containsExactly("Zenith Supply", "Globex Trading", "Acme Sellers");
    }

    @Test
    void listSellersPaginationCountsOnlyFilteredSellers() {
        UUID workspaceId = UUID.randomUUID();
        sellerRepository.save(Seller.build(workspaceId, "Acme Sellers", "11111111111"));
        sellerRepository.save(Seller.build(workspaceId, "Acme Logistics", "22222222222"));
        sellerRepository.save(Seller.build(workspaceId, "Globex Trading", "33333333333"));
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<SellerPage> response = restTemplate.exchange(
                "/order/seller?name=acme&page=0&size=1", HttpMethod.GET,
                authenticatedRequest(cookie), SellerPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().totalElements()).isEqualTo(2);
        assertThat(response.getBody().totalPages()).isEqualTo(2);
    }

    @Test
    void adminCanRenameSellerFromOwnWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Old Name", "33333333333"));
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        UpdateSellerRequest request = new UpdateSellerRequest("New Name");
        HttpEntity<UpdateSellerRequest> httpRequest = authenticatedJsonRequest(adminCookie, request);
        ResponseEntity<SellerResponse> response = restTemplate.exchange(
                "/order/seller/" + seller.getSellerId(), HttpMethod.PUT, httpRequest, SellerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("New Name");
    }

    @Test
    void adminCannotRenameSellerFromAnotherWorkspace() {
        UUID ownerWorkspace = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(ownerWorkspace, "Old Name", "44444444444"));
        String otherAdminCookie = cookiePairFor(UUID.randomUUID(), "OP-002", UUID.randomUUID(), "ADMIN");

        UpdateSellerRequest request = new UpdateSellerRequest("Hijacked Name");
        HttpEntity<UpdateSellerRequest> httpRequest = authenticatedJsonRequest(otherAdminCookie, request);
        ResponseEntity<String> response = restTemplate.exchange(
                "/order/seller/" + seller.getSellerId(), HttpMethod.PUT, httpRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanDeleteSellerWithoutOrders() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Deletable", "55555555555"));
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/order/seller/" + seller.getSellerId(), HttpMethod.DELETE,
                authenticatedRequest(adminCookie), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(sellerRepository.findById(seller.getSellerId())).isEmpty();
    }

    @Test
    void adminCannotDeleteSellerWithExistingOrders() {
        UUID workspaceId = UUID.randomUUID();
        Seller seller = sellerRepository.save(Seller.build(workspaceId, "Has Orders", "66666666666"));
        Address address = new Address("Main St", "100", "Downtown", "Springfield", "SP", "01000-000");
        Party party = new Party("John Doe", "99988877766", address);
        orderRepository.save(Order.build(
                workspaceId, "John Doe", 7001L, seller.getSellerId(), party, party, Map.of("SKU-1", 1)));
        String adminCookie = cookiePairFor(UUID.randomUUID(), "OP-001", workspaceId, "ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/order/seller/" + seller.getSellerId(), HttpMethod.DELETE,
                authenticatedRequest(adminCookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sellerRepository.findById(seller.getSellerId())).isPresent();
    }
}
