package com.equisibe.model;

import jakarta.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "promotional_codes")
public class PromotionalCode {
    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false)
    private int percentage;

    @Column(nullable = false)
    private boolean active = true;

    protected PromotionalCode() {
    }

    public PromotionalCode(String code, int percentage) {
        if (percentage < 1 || percentage > 99) {
            throw new IllegalArgumentException("El descuento debe estar entre 1 y 99.");
        }
        this.code = code.trim().toUpperCase(Locale.ROOT);
        this.percentage = percentage;
    }

    public String getCode() { return code; }
    public int getPercentage() { return percentage; }
    public boolean isActive() { return active; }

    public void setActive(boolean active) {
        this.active = active;
    }
}
