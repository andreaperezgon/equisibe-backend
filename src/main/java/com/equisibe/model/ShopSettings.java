package com.equisibe.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shop_settings")
public class ShopSettings {
    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean closed = false;

    @Column(nullable = false, length = 120)
    private String title = "Estamos preparando algo especial";

    @Column(nullable = false, length = 2000)
    private String text = "La tienda está cerrada temporalmente. ¡Volvemos pronto!";

    @Column(nullable = false)
    private boolean countdownEnabled = false;

    private Instant countdownTarget;

    public ShopSettings() {
    }

    public void update(
            boolean closed, String title, String text,
            boolean countdownEnabled, Instant countdownTarget) {
        this.closed = closed;
        this.title = title.trim();
        this.text = text.trim();
        this.countdownEnabled = countdownEnabled;
        this.countdownTarget = countdownEnabled ? countdownTarget : null;
    }

    public Long getId() { return id; }
    public boolean isClosed() { return closed; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public boolean isCountdownEnabled() { return countdownEnabled; }
    public Instant getCountdownTarget() { return countdownTarget; }
}
