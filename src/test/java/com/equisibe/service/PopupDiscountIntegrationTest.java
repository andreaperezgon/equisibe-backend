package com.equisibe.service;

import com.equisibe.model.PromotionalCode;
import com.equisibe.model.PromotionalPopup;
import com.equisibe.repository.PromotionalCodeRepository;
import com.equisibe.repository.PromotionalPopupRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PopupDiscountIntegrationTest {
    @Autowired PromotionalCodeRepository codes;
    @Autowired PromotionalPopupRepository popups;
    @Autowired PromotionalCodeService service;

    String code() {
        String code = "TEST" + UUID.randomUUID().toString().replace("-", "");
        return codes.saveAndFlush(new PromotionalCode(code, 10)).getCode();
    }
    PromotionalPopup popup(String code, boolean active) {
        var popup = new PromotionalPopup("Promoción", "Texto de prueba", "", "", "");
        popup.setPromoCode(code);
        popup.setActive(active);
        return popups.saveAndFlush(popup);
    }
    void unavailable(String code) {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.getAvailablePercentage(code)).getStatusCode().value());
    }
    @Test void discountTracksPopupActivationDeactivationAndDeletion() {
        String code = code();
        unavailable(code);
        var popup = popup(code, false);
        unavailable(code);
        popup.setActive(true);
        popups.saveAndFlush(popup);
        assertEquals(10, service.getAvailablePercentage(" " + code.toLowerCase(java.util.Locale.ROOT) + " "));
        popup.setActive(false);
        popups.saveAndFlush(popup);
        unavailable(code);
        popup.setActive(true);
        popups.saveAndFlush(popup);
        assertEquals(10, service.getAvailablePercentage(code));
        popups.delete(popup);
        popups.flush();
        unavailable(code);
    }
    @Test void anotherActivePopupCannotEnableAnUnrelatedCode() {
        String first = code();
        String second = code();
        popup(first, true);
        popup(second, false);
        assertEquals(10, service.getAvailablePercentage(first));
        unavailable(second);
    }
    @Test void discountRemainsAvailableWhileAnotherPopupWithSameCodeIsActive() {
        String code = code();
        var first = popup(code, true);
        popup(code, true);
        first.setActive(false);
        popups.saveAndFlush(first);
        assertEquals(10, service.getAvailablePercentage(code));
    }
    @Test void inactiveDiscountCodeCannotBeEnabledByAPopup() {
        String code = code();
        popup(code, true);
        var discount = codes.findById(code).orElseThrow();
        discount.setActive(false);
        codes.saveAndFlush(discount);
        unavailable(code);
    }
    @Test void popupConfigurationCanValidateCodeBeforeActivatingItsPopup() {
        String code = code();
        popup(code, false);
        assertEquals(10, service.getPercentage(code));
        unavailable(code);
        assertEquals(0, service.getAvailablePercentage(null));
        assertEquals(0, service.getAvailablePercentage(" "));
    }
}
