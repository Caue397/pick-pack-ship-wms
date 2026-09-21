package com.pickpackship.order.service;

import com.pickpackship.order.api.dto.CreateSellerRequest;
import com.pickpackship.order.api.dto.SellerFilter;
import com.pickpackship.order.api.dto.SellerResponse;
import com.pickpackship.order.api.dto.UpdateSellerRequest;
import com.pickpackship.order.domain.Seller;
import com.pickpackship.order.exception.DuplicateSellerDocumentException;
import com.pickpackship.order.exception.SellerHasOrdersException;
import com.pickpackship.order.exception.SellerNotExistsException;
import com.pickpackship.order.repository.OrderRepository;
import com.pickpackship.order.repository.SellerRepository;
import com.pickpackship.order.repository.SellerSpecifications;
import com.pickpackship.order.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final OrderRepository orderRepository;

    public Page<SellerResponse> listSellers(Pageable pageable, SellerFilter filter, AuthenticatedUser caller) {
        Specification<Seller> spec = Specification
                .where(SellerSpecifications.hasWorkspaceId(caller.workspaceId()))
                .and(SellerSpecifications.hasName(filter.name()))
                .and(SellerSpecifications.hasDocument(filter.document()));

        Page<Seller> sellers = sellerRepository.findAll(spec, pageable);

        return sellers.map(this::toResponse);
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

