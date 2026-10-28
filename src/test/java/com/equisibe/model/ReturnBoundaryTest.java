package com.equisibe.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReturnBoundaryTest {
    ReturnRequest request() {
        return new ReturnRequest(UUID.randomUUID(), mock(Order.class), ReturnType.WITHDRAWAL,
                null, List.of(new ReturnItem(0, 1)));
    }
    @ParameterizedTest @CsvSource({
        "REQUESTED,IN_REVIEW", "REQUESTED,AWAITING_ITEMS", "REQUESTED,REJECTED",
        "IN_REVIEW,AWAITING_ITEMS", "IN_REVIEW,REJECTED", "AWAITING_ITEMS,RECEIVED", "RECEIVED,CLOSED"
    })
    void allowsDocumentedTransitions(ReturnStatus current, ReturnStatus next) {
        var request = request();
        ReflectionTestUtils.setField(request, "status", current);
        request.updateStatus(next, " Respuesta del taller ");
        assertEquals(next, request.getStatus());
        assertEquals("Respuesta del taller", request.getAdminNote());
    }
    @ParameterizedTest @CsvSource({
        "REQUESTED,RECEIVED", "REQUESTED,CLOSED", "IN_REVIEW,REQUESTED", "IN_REVIEW,RECEIVED", "IN_REVIEW,CLOSED",
        "AWAITING_ITEMS,REQUESTED", "AWAITING_ITEMS,IN_REVIEW", "AWAITING_ITEMS,CLOSED", "AWAITING_ITEMS,REJECTED",
        "RECEIVED,REQUESTED", "RECEIVED,IN_REVIEW", "RECEIVED,AWAITING_ITEMS", "RECEIVED,REJECTED",
        "CLOSED,REQUESTED", "CLOSED,RECEIVED", "REJECTED,IN_REVIEW", "REJECTED,CLOSED"
    })
    void refusesSkippingStagesReopeningOrReversingProgress(ReturnStatus current, ReturnStatus next) {
        var request = request();
        ReflectionTestUtils.setField(request, "status", current);
        assertThrows(IllegalStateException.class, () -> request.updateStatus(next, "Respuesta"));
        assertEquals(current, request.getStatus());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
    void terminalStatesRequireAnExplanation(String note) {
        var request = request();
        assertThrows(IllegalArgumentException.class, () -> request.updateStatus(ReturnStatus.REJECTED, note));
        ReflectionTestUtils.setField(request, "status", ReturnStatus.RECEIVED);
        assertThrows(IllegalArgumentException.class, () -> request.updateStatus(ReturnStatus.CLOSED, note));
    }
    @Test void permitsUpdatingNoteWithoutChangingStateAndNormalizesMissingNote() {
        var request = request();
        assertEquals("", request.getNote());
        request.updateStatus(ReturnStatus.REQUESTED, null);
        assertEquals("", request.getAdminNote());
        request.updateStatus(ReturnStatus.REQUESTED, " Nueva nota ");
        assertEquals("Nueva nota", request.getAdminNote());
        assertThrows(NullPointerException.class, () -> request.updateStatus(null, "Nota"));
    }
    @Test void copiesSelectedItemsAndRequiresAtLeastOne() {
        var items = new ArrayList<>(List.of(new ReturnItem(0, 1)));
        var order = mock(Order.class);
        var request = new ReturnRequest(UUID.randomUUID(), order, ReturnType.WITHDRAWAL, " Motivo ", items);
        items.clear();
        assertEquals(1, request.getItems().size());
        assertEquals("Motivo", request.getNote());
        assertThrows(UnsupportedOperationException.class, () -> request.getItems().clear());
        assertThrows(IllegalArgumentException.class,
                () -> new ReturnRequest(UUID.randomUUID(), order, ReturnType.WITHDRAWAL, "", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ReturnRequest(UUID.randomUUID(), order, ReturnType.WITHDRAWAL, "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ReturnItem(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ReturnItem(0, 0));
    }
}
