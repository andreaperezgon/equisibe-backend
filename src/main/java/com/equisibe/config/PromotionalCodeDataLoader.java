package com.equisibe.config;

import com.equisibe.model.PromotionalCode;
import com.equisibe.repository.PromotionalCodeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PromotionalCodeDataLoader implements ApplicationRunner {
    private final PromotionalCodeRepository codes;

    public PromotionalCodeDataLoader(PromotionalCodeRepository codes) {
        this.codes = codes;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("EQUISIBE10", 10);
        createIfMissing("EQUISIBE20", 20);
    }

    private void createIfMissing(String code, int percentage) {
        if (!codes.existsById(code)) {
            codes.save(new PromotionalCode(code, percentage));
        }
    }
}
