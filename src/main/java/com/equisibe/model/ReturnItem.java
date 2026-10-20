package com.equisibe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ReturnItem {
    @Column(nullable = false)
    private int lineIndex;

    @Column(nullable = false)
    private long quantity;

    protected ReturnItem() {
    }

    public ReturnItem(int lineIndex, long quantity) {
        if (lineIndex < 0 || quantity <= 0) {
            throw new IllegalArgumentException("La prenda o cantidad no es válida.");
        }
        this.lineIndex = lineIndex;
        this.quantity = quantity;
    }

    public int getLineIndex() { return lineIndex; }
    public long getQuantity() { return quantity; }
}
