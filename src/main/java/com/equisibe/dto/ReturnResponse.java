package com.equisibe.dto;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.ReturnRequest;
import com.equisibe.model.ReturnStatus;
import com.equisibe.model.ReturnType;
import java.time.Instant;
import java.util.List;

public record ReturnResponse(
        Long id,
        Long orderId,
        ReturnType type,
        ReturnStatus status,
        String note,
        String adminNote,
        Instant createdAt,
        Instant updatedAt,
        List<Item> items
) {
    public static ReturnResponse from(ReturnRequest request) {
        var order = request.getOrder();
        var items = request.getItems().stream().map(item -> {
            var original = order.getItems().get(item.getLineIndex());
            return new Item(
                    item.getLineIndex(),
                    original.getProductName(),
                    original.getSize(),
                    item.getQuantity()
            );
        }).toList();

        return new ReturnResponse(
                request.getId(), order.getId(),
                request.getType(), request.getStatus(),
                request.getNote(), request.getAdminNote(),
                request.getCreatedAt(), request.getUpdatedAt(),
                items
        );
    }

    public record Item(
            int lineIndex,
            String productName,
            ClothingSize size,
            long quantity
    ) {
    }
}
