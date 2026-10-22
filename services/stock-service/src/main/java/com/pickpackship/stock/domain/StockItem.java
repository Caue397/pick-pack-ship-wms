package com.pickpackship.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * A physical warehouse position, fixed to one product (N:1 with Product, one product per position).
 * It can exist with zero balance; quantities only change through receipt, reservation and putaway.
 */
@Entity
@Table(
        name = "stock_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_item_position",
                columnNames = {"workspace_id", "location_block", "location_aisle", "location_column", "location_level"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "stock_item_id")
    private UUID stockItemId;

    @Column(nullable = false, name = "workspace_id")
    private UUID workspaceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stock_items_product"))
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private Product product;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "block", column = @Column(name = "location_block", nullable = false)),
            @AttributeOverride(name = "aisle", column = @Column(name = "location_aisle", nullable = false)),
            @AttributeOverride(name = "column", column = @Column(name = "location_column", nullable = false)),
            @AttributeOverride(name = "level", column = @Column(name = "location_level", nullable = false))
    })
    private Location location;

    @Column(nullable = false, name = "available_master_boxes")
    @Builder.Default
    private int availableMasterBoxes = 0;

    @Column(nullable = false, name = "available_inner_boxes")
    @Builder.Default
    private int availableInnerBoxes = 0;

    @Column(nullable = false, name = "available_loose_units")
    @Builder.Default
    private int availableLooseUnits = 0;

    @Column(nullable = false, name = "reserved_quantity")
    @Builder.Default
    private int reservedQuantity = 0;

    /** When the position left zero balance; FIFO key for reservation. */
    @Column(name = "stocked_since")
    private Instant stockedSince;
}
