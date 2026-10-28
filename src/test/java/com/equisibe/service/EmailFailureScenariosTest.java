package com.equisibe.service;

import com.equisibe.model.Contact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailFailureScenariosTest {
    final JavaMailSender sender = mock(JavaMailSender.class);
    final Contact contact = new Contact("Cliente", "client@example.com", "Consulta");
    EmailService service(boolean enabled, String from, String to) {
        return new EmailService(sender, enabled, from, to);
    }
    void unavailable(org.junit.jupiter.api.function.Executable action) {
        assertEquals(503, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }
    @ParameterizedTest @ValueSource(strings={"", " "})
    void rejectsMissingSenderForContactAndPasswordReset(String from) {
        var service = service(true, from, "destination@example.com");
        unavailable(() -> service.sendContact(contact));
        unavailable(service::requireAvailable);
        unavailable(() -> service.sendPasswordReset("client@example.com", "https://example.com/reset"));
        verifyNoInteractions(sender);
    }
    @ParameterizedTest @ValueSource(strings={"", " "})
    void contactRequiresDestinationButPasswordResetDoesNot(String to) {
        var service = service(true, "sender@example.com", to);
        unavailable(() -> service.sendContact(contact));
        service.requireAvailable();
        service.sendPasswordReset("client@example.com", "https://example.com/reset?token=test");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertArrayEquals(new String[]{"client@example.com"}, captor.getValue().getTo());
        assertEquals("sender@example.com", captor.getValue().getFrom());
        assertTrue(captor.getValue().getText().contains("https://example.com/reset?token=test"));
        assertTrue(captor.getValue().getText().contains("30 minutos"));
    }
    @ParameterizedTest @NullSource @ValueSource(strings={"bad\raddress@example.com", "bad\naddress@example.com"})
    void rejectsInvalidReplyHeaders(String email) {
        var invalid = new Contact("Cliente", email, "Consulta");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service(true, "sender@example.com", "destination@example.com").sendContact(invalid))
                .getStatusCode().value());
        verifyNoInteractions(sender);
    }
    @Test void unavailableResetDoesNotSendAndSmtpFailureIsReportedWithoutDetails() {
        unavailable(() -> service(false, "sender@example.com", "destination@example.com")
                .sendPasswordReset("client@example.com", "https://example.com/reset"));
        doThrow(new MailSendException("internal SMTP details")).when(sender).send(any(SimpleMailMessage.class));
        var failure = assertThrows(ResponseStatusException.class,
                () -> service(true, "sender@example.com", "destination@example.com")
                        .sendPasswordReset("client@example.com", "https://example.com/reset"));
        assertEquals(503, failure.getStatusCode().value());
        assertFalse(failure.getReason().contains("SMTP"));
    }
}
