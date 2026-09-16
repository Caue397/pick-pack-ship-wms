package com.pickpackship.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    public static Order build(
        UUID workspaceId,
        String customerName,
        Long orderNumber,
        UUID seller,
        Party sender,
        Party recipient,
        Map<String, Integer> items
    ) {
        Order order = new Order();
        order.workspaceId = workspaceId;
        order.customerName = customerName;
        order.orderNumber = orderNumber;
        order.seller = seller;
        order.sender = sender;
        order.recipient = recipient;
        order.items = items;
        order.status = OrderStatus.CREATED;
        return order;
    }

    public void cancel(String cancellationReason) {
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = cancellationReason;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "order_number")
    private Long orderNumber;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "seller", nullable = false)
    private UUID seller;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "sender_name")),
            @AttributeOverride(name = "document", column = @Column(name = "sender_document")),
            @AttributeOverride(name = "address.street", column = @Column(name = "sender_street")),
            @AttributeOverride(name = "address.number", column = @Column(name = "sender_number")),
            @AttributeOverride(name = "address.neighborhood", column = @Column(name = "sender_neighborhood")),
            @AttributeOverride(name = "address.city", column = @Column(name = "sender_city")),
            @AttributeOverride(name = "address.state", column = @Column(name = "sender_state")),
            @AttributeOverride(name = "address.zipCode", column = @Column(name = "sender_zip_code"))
    })
    private Party sender;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "recipient_name")),
            @AttributeOverride(name = "document", column = @Column(name = "recipient_document")),
            @AttributeOverride(name = "address.street", column = @Column(name = "recipient_street")),
            @AttributeOverride(name = "address.number", column = @Column(name = "recipient_number")),
            @AttributeOverride(name = "address.neighborhood", column = @Column(name = "recipient_neighborhood")),
            @AttributeOverride(name = "address.city", column = @Column(name = "recipient_city")),
            @AttributeOverride(name = "address.state", column = @Column(name = "recipient_state")),
            @AttributeOverride(name = "address.zipCode", column = @Column(name = "recipient_zip_code"))
    })
    private Party recipient;

    @ElementCollection
    @CollectionTable(
            name = "order_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @MapKeyColumn(name = "sku")
    @Column(name = "quantity")
    private Map<String, Integer> items;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
