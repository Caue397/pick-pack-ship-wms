package com.pickpackship.order.service;

import com.pickpackship.order.api.dto.*;
import com.pickpackship.order.domain.Order;
import com.pickpackship.order.domain.Seller;
import com.pickpackship.order.exception.OrderNotFoundException;
import com.pickpackship.order.exception.SellerNotExistsException;
import com.pickpackship.order.outbox.OrderCancelledPayload;
import com.pickpackship.order.outbox.OrderCreatedPayload;
import com.pickpackship.order.outbox.OutboxWriter;
import com.pickpackship.order.repository.OrderRepository;
import com.pickpackship.order.repository.SellerRepository;
import com.pickpackship.order.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final OutboxWriter outboxWriter;

//    public List<SummaryOrderResponse> listOrders(AuthenticatedUser caller) {
//        return orderRepository.findByWorkspaceId(caller.workspaceId())
//                .stream()
//                .map(this::)
//                .toList();
//    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, AuthenticatedUser caller) {
        if (sellerRepository.findById(request.seller()).isEmpty()) {
            throw new SellerNotExistsException();
        }

        Order order = Order.build(
                caller.workspaceId(),
                request.customerName(),
                request.orderNumber(),
                request.seller(),
                request.sender(),
                request.recipient(),
                request.items()
        );

        orderRepository.save(order);

        outboxWriter.write(
                "order.created",
                new OrderCreatedPayload(order.getOrderId(), order.getWorkspaceId(), order.getItems())
        );

        return toResponse(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, CancelOrderRequest request, AuthenticatedUser caller) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getWorkspaceId().equals(caller.workspaceId())) {
            throw new AccessDeniedException("You can only cancel a order from your workspace");
        }

        order.cancel(request.cancellationReason());
        orderRepository.save(order);

        outboxWriter.write(
                "order.cancelled",
                new OrderCancelledPayload(order.getOrderId(), order.getWorkspaceId(), order.getItems())
        );
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getWorkspaceId(),
                order.getCustomerName(),
                order.getSeller(),
                order.getSender(),
                order.getRecipient(),
                order.getItems()
        );
    }
}
