package com.pickpackship.stock;

import com.pickpackship.stock.domain.Location;
import com.pickpackship.stock.domain.Product;
import com.pickpackship.stock.domain.StockItem;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No stock endpoint exists yet (POST /stock/product depends on seller.created from order-service),
 * so this covers the foundation: security wiring and the database constraints from the spec.
 */
class StockFoundationIntegrationTest extends AbstractIntegrationTest {

    private Product newProduct(UUID workspaceId, UUID sellerId, String sku, String ean) {
        return productRepository.saveAndFlush(Product.builder()
                .workspaceId(workspaceId)
                .sellerId(sellerId)
                .sku(sku)
                .eanCode(ean)
                .name("Product " + sku)
                .build());
    }

    private StockItem newPosition(UUID workspaceId, Product product, String block) {
        return StockItem.builder()
                .workspaceId(workspaceId)
                .product(product)
                .location(new Location(block, "A1", "C1", "L1"))
                .build();
    }

    @Test
    void requestWithoutTokenReturns401() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/stock/availability", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedRequestPassesSecurityAndUnknownPathReturnsProblemDetail404() {
        String cookie = cookiePairFor(UUID.randomUUID(), "OP-001", UUID.randomUUID(), "ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/stock/does-not-exist", HttpMethod.GET, authenticatedRequest(cookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType().toString()).contains("problem+json");
    }

    @Test
    void sameSkuOrEanTwiceForTheSameSellerIsRejected() {
        UUID workspaceId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        newProduct(workspaceId, sellerId, "SKU-1", "7891000100103");

        assertThatThrownBy(() -> newProduct(workspaceId, sellerId, "SKU-1", "7891000100110"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_products_seller_sku");
        assertThatThrownBy(() -> newProduct(workspaceId, sellerId, "SKU-2", "7891000100103"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_products_seller_ean");
    }

    @Test
    void sameSkuAndEanAreAllowedForDifferentSellers() {
        UUID workspaceId = UUID.randomUUID();
        newProduct(workspaceId, UUID.randomUUID(), "SKU-1", "7891000100103");
        newProduct(workspaceId, UUID.randomUUID(), "SKU-1", "7891000100103");

        assertThat(productRepository.count()).isEqualTo(2);
    }

    @Test
    void positionIsUniquePerWorkspaceButAProductCanHaveSeveralPositions() {
        UUID workspaceId = UUID.randomUUID();
        Product product = newProduct(workspaceId, UUID.randomUUID(), "SKU-1", "7891000100103");

        stockRepository.saveAndFlush(newPosition(workspaceId, product, "B1"));
        stockRepository.saveAndFlush(newPosition(workspaceId, product, "B2"));
        // same coordinates in another workspace are a different physical warehouse
        Product other = newProduct(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "7891000100103");
        stockRepository.saveAndFlush(newPosition(other.getWorkspaceId(), other, "B1"));

        assertThatThrownBy(() -> stockRepository.saveAndFlush(newPosition(workspaceId, product, "B1")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_stock_item_position");
    }

    @Test
    void productWithAPositionCannotBeDeleted() {
        UUID workspaceId = UUID.randomUUID();
        Product product = newProduct(workspaceId, UUID.randomUUID(), "SKU-1", "7891000100103");
        stockRepository.saveAndFlush(newPosition(workspaceId, product, "B1"));

        assertThatThrownBy(() -> productRepository.deleteById(product.getProductId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
