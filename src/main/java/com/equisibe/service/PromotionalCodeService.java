package com.equisibe.service;

import com.equisibe.repository.PromotionalCodeRepository;
import com.equisibe.repository.PromotionalPopupRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PromotionalCodeService {
    private final PromotionalCodeRepository codes;
    private final PromotionalPopupRepository popups;

    public PromotionalCodeService(PromotionalCodeRepository codes, PromotionalPopupRepository popups) {
        this.codes = codes;
        this.popups = popups;
    }

    public int getAvailablePercentage(String value) {
        int percentage = getPercentage(value);
        if (percentage == 0) return 0;

        String code = value.trim().toUpperCase(Locale.ROOT);
        if (!popups.existsByPromoCodeAndActiveTrue(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este código promocional no está disponible en este momento."
            );
        }
        return percentage;
    }
    public int getPercentage(String value) {
        if (value == null || value.isBlank()) return 0;
        String code = value.trim().toUpperCase(Locale.ROOT);
        return codes.findById(code)
                .filter(item -> item.isActive())
                .map(item -> item.getPercentage())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El código promocional no existe o está desactivado."
                ));
    }
}
