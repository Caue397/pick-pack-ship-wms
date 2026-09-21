package com.pickpackship.order.repository;

import com.pickpackship.order.domain.Order;
import com.pickpackship.order.domain.OrderStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class OrderSpecifications {

    public static Specification<Order> fetchSeller() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("sellerRef", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Order> hasWorkspaceId(UUID workspaceId) {
        return (root, query, cb) -> cb.equal(root.get("workspaceId"), workspaceId);
    }

    public static Specification<Order> hasOrderNumber(Long orderNumber) {
        return orderNumber == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("orderNumber"), orderNumber);
    }

    public static Specification<Order> hasCustomerName(String customerName) {
        return customerName == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.like(cb.lower(root.get("customerName")), "%" + customerName.toLowerCase() + "%");
    }

    public static Specification<Order> hasSeller(UUID seller) {
        return seller == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("seller"), seller);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return status == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
