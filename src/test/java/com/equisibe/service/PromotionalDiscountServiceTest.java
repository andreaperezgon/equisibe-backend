package com.equisibe.service;

import com.equisibe.dto.CartValidationResponse;
import com.equisibe.model.ClothingSize;
import com.equisibe.model.PromotionalCode;
import com.equisibe.repository.PromotionalCodeRepository;
import com.equisibe.repository.PromotionalPopupRepository;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromotionalDiscountServiceTest {
    private final PromotionalCodeRepository repository =
            mock(PromotionalCodeRepository.class);
    private final PromotionalPopupRepository popups = mock(PromotionalPopupRepository.class);
    private final PromotionalDiscountService service =
            new PromotionalDiscountService(
                    new PromotionalCodeService(repository, popups),
                    new ShippingCostService()
            );

    @BeforeEach
    void activePromotions() {
        when(popups.existsByPromoCodeAndActiveTrue("EQUISIBE10")).thenReturn(true);
        when(popups.existsByPromoCodeAndActiveTrue("EQUISIBE20")).thenReturn(true);
    }

    @Test
    void rejectsDiscountWhenItsPopupIsInactiveOrMissing() {
        when(repository.findById("EQUISIBE10"))
                .thenReturn(Optional.of(new PromotionalCode("EQUISIBE10", 10)));
        when(popups.existsByPromoCodeAndActiveTrue("EQUISIBE10")).thenReturn(false);
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.apply(cart("54.90", 1), " equisibe10 "));
        assertEquals(400, error.getStatusCode().value());
        verify(popups).existsByPromoCodeAndActiveTrue("EQUISIBE10");
    }

    @Test
    void activationIsRecheckedEveryTimeCodeIsApplied() {
        when(repository.findById("EQUISIBE10"))
                .thenReturn(Optional.of(new PromotionalCode("EQUISIBE10", 10)));
        assertEquals("EQUISIBE10", service.apply(cart("54.90", 1), "EQUISIBE10").promoCode());
        when(popups.existsByPromoCodeAndActiveTrue("EQUISIBE10")).thenReturn(false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.apply(cart("54.90", 1), "EQUISIBE10"));
        when(popups.existsByPromoCodeAndActiveTrue("EQUISIBE10")).thenReturn(true);
        assertEquals("EQUISIBE10", service.apply(cart("54.90", 1), "EQUISIBE10").promoCode());
    }
    private CartValidationResponse cart(String price, int quantity) {
        var unit = new BigDecimal(price);
        var subtotal = unit.multiply(BigDecimal.valueOf(quantity));
        var shipping = new ShippingCostService().calculate(subtotal);
        return new CartValidationResponse(
                true,
                List.of(new CartValidationResponse.Item(
                        1L, "Prenda", ClothingSize.S,
                        quantity, unit, subtotal
                )),
                List.of(), subtotal, shipping,
                subtotal.add(shipping), "EUR"
        );
    }

    @Test
    void appliesTenPercentAndNormalizesCode() {
        when(repository.findById("EQUISIBE10"))
                .thenReturn(Optional.of(new PromotionalCode("EQUISIBE10", 10)));
        var result = service.apply(cart("54.90", 1), " equisibe10 ");
        assertEquals("EQUISIBE10", result.promoCode());
        assertEquals(new BigDecimal("49.41"), result.subtotal());
        assertEquals(new BigDecimal("5.49"), result.discountAmount());
        assertEquals(new BigDecimal("53.36"), result.total());
    }

    @Test
    void appliesTwentyPercentAndRecalculatesShipping() {
        when(repository.findById("EQUISIBE20"))
                .thenReturn(Optional.of(new PromotionalCode("EQUISIBE20", 20)));
        var result = service.apply(cart("80.00", 1), "EQUISIBE20");
        assertEquals(new BigDecimal("64.00"), result.subtotal());
        assertEquals(new BigDecimal("3.95"), result.shippingCost());
        assertEquals(new BigDecimal("67.95"), result.total());
    }

    @Test
    void keepsUnitPricesAndTotalsConsistentAfterRounding() {
        when(repository.findById("EQUISIBE10"))
                .thenReturn(Optional.of(new PromotionalCode("EQUISIBE10", 10)));
        var result = service.apply(cart("19.99", 3), "EQUISIBE10");
        assertEquals(new BigDecimal("17.99"), result.items().getFirst().unitPrice());
        assertEquals(new BigDecimal("53.97"), result.subtotal());
        assertEquals(
                result.subtotal(),
                result.items().getFirst().unitPrice()
                        .multiply(new BigDecimal("3"))
        );
    }

    @Test
    void leavesCartUnchangedWithoutCode() {
        var cart = cart("54.90", 1);
        assertSame(cart, service.apply(cart, ""));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnknownAndInactiveCodes() {
        when(repository.findById("INVALIDO")).thenReturn(Optional.empty());
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.apply(cart("54.90", 1), "INVALIDO")
        );

        var inactive = new PromotionalCode("EQUISIBE10", 10);
        inactive.setActive(false);
        when(repository.findById("EQUISIBE10")).thenReturn(Optional.of(inactive));
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.apply(cart("54.90", 1), "EQUISIBE10")
        );
    }
}
