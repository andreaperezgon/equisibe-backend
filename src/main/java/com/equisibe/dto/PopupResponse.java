package com.equisibe.dto;

import com.equisibe.model.PromotionalPopup;
import java.time.Instant;

public record PopupResponse(
        Long id,
        Long version,
        String title,
        String text,
        String promoCode,
        boolean active,
        Instant updatedAt
) {
    public static PopupResponse from(PromotionalPopup popup) {
        return new PopupResponse(
                popup.getId(), popup.getVersion(),
                popup.getTitle(), popup.getText(),
                popup.getPromoCode(), popup.isActive(),
                popup.getUpdatedAt()
        );
    }
}
