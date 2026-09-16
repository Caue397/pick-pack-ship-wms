package com.pickpackship.order.service;

import com.pickpackship.order.api.dto.CreateSellerRequest;
import com.pickpackship.order.api.dto.SellerResponse;
import com.pickpackship.order.api.dto.UpdateSellerRequest;
import com.pickpackship.order.domain.Seller;
import com.pickpackship.order.exception.DuplicateSellerDocumentException;
import com.pickpackship.order.exception.SellerHasOrdersException;
import com.pickpackship.order.exception.SellerNotExistsException;
import com.pickpackship.order.repository.OrderRepository;
import com.pickpackship.order.repository.SellerRepository;
import com.pickpackship.order.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final OrderRepository orderRepository;

    public List<SellerResponse> listSellers(AuthenticatedUser caller) {
        return sellerRepository.findByWorkspaceId(caller.workspaceId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SellerResponse createSeller(CreateSellerRequest request, AuthenticatedUser caller) {
        if (sellerRepository.findByDocument(request.document()).isPresent()) {
            throw new DuplicateSellerDocumentException();
        }

        Seller seller = Seller.build(
                caller.workspaceId(),
                request.name(),
                request.document()
        );

        sellerRepository.save(seller);

        return toResponse(seller);
    }

    public SellerResponse updateSeller(UpdateSellerRequest request, UUID sellerId, AuthenticatedUser caller) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(SellerNotExistsException::new);

        if (!seller.getWorkspaceId().equals(caller.workspaceId())) {
            throw new AccessDeniedException("You can only update a seller from your workspace");
        }

        seller.rename(request.name());
        sellerRepository.save(seller);

        return toResponse(seller);
    }

    public void deleteSeller(UUID sellerId, AuthenticatedUser caller) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(SellerNotExistsException::new);

        if (!seller.getWorkspaceId().equals(caller.workspaceId())) {
            throw new AccessDeniedException("You can only delete a seller from your workspace");
        }

        if (orderRepository.existsBySeller(sellerId)) {
            throw new SellerHasOrdersException();
        }

        sellerRepository.deleteById(sellerId);
    }

    private SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getSellerId(),
                seller.getWorkspaceId(),
                seller.getName(),
                seller.getDocument()
        );
    }
}

