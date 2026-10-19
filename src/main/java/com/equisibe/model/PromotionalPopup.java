package com.equisibe.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "promotional_popups")
public class PromotionalPopup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false, length = 1000)
    private String imageUrl = "";

    @Column(nullable = false, length = 60)
    private String buttonText = "";

    @Column(nullable = false, length = 500)
    private String targetUrl = "";

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PromotionalPopup() {
    }

    public PromotionalPopup(
            String title, String text, String imageUrl,
            String buttonText, String targetUrl) {
        updateDetails(title, text, imageUrl, buttonText, targetUrl);
    }

    public void updateDetails(
            String title, String text, String imageUrl,
            String buttonText, String targetUrl) {
        this.title = title.trim();
        this.text = text.trim();
        this.imageUrl = imageUrl == null ? "" : imageUrl.trim();
        this.buttonText = buttonText == null ? "" : buttonText.trim();
        this.targetUrl = targetUrl == null ? "" : targetUrl.trim();
    }


    @Column(length = 40)
    private String promoCode = "";

    public String getPromoCode() {
        return promoCode == null ? "" : promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @PrePersist
    @PreUpdate
    private void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }
    public String getButtonText() { return buttonText; }
    public String getTargetUrl() { return targetUrl; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
}
