package com.pickpackship.order.api;

import com.pickpackship.order.api.dto.CreateSellerRequest;
import com.pickpackship.order.api.dto.SellerResponse;
import com.pickpackship.order.api.dto.UpdateSellerRequest;
import com.pickpackship.order.security.AuthenticatedUser;
import com.pickpackship.order.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order/seller")
@RequiredArgsConstructor
public class SellerController {
    private final SellerService sellerService;

    @GetMapping
    public ResponseEntity<List<SellerResponse>> listSellers(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        return ResponseEntity.ok(sellerService.listSellers(caller));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerResponse> createSeller(
            @Valid @RequestBody CreateSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        SellerResponse response = sellerService.createSeller(request, caller);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerResponse> updateSeller(
            @PathVariable UUID sellerId,
            @RequestBody UpdateSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        return ResponseEntity.ok(sellerService.updateSeller(request, sellerId, caller));
    }

    @DeleteMapping("/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeller(
            @PathVariable UUID sellerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        sellerService.deleteSeller(sellerId, caller);
        return ResponseEntity.noContent().build();
    }
}
