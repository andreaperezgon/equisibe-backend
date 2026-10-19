package com.equisibe.service;

import com.equisibe.repository.PromotionalCodeRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PromotionalCodeService {
    private final PromotionalCodeRepository codes;

    public PromotionalCodeService(PromotionalCodeRepository codes) {
        this.codes = codes;
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
