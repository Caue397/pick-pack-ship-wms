package com.pickpackship.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * SKU and EAN are unique per seller only (3PL: two sellers can sell the same physical item),
 * so workspace_id stays out of the constraints; sellerId already belongs to a single workspace.
 */
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_seller_sku", columnNames = {"seller_id", "sku"}),
                @UniqueConstraint(name = "uk_products_seller_ean", columnNames = {"seller_id", "ean_code"})
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false, name = "workspace_id")
    private UUID workspaceId;

    @Column(nullable = false, name = "seller_id")
    private UUID sellerId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false, name = "ean_code")
    private String eanCode;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "weight_g")
    private Integer weightG;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "heightMm", column = @Column(name = "height_mm")),
            @AttributeOverride(name = "widthMm", column = @Column(name = "width_mm")),
            @AttributeOverride(name = "depthMm", column = @Column(name = "depth_mm"))
    })
    private Dimensions dimensions;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "innerBoxBarcode", column = @Column(name = "inner_box_barcode")),
            @AttributeOverride(name = "unitsPerInnerBox", column = @Column(name = "units_per_inner_box")),
            @AttributeOverride(name = "masterBoxBarcode", column = @Column(name = "master_box_barcode")),
            @AttributeOverride(name = "unitsPerMasterBox", column = @Column(name = "units_per_master_box"))
    })
    private Packaging packaging;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
