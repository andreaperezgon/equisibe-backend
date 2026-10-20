package com.equisibe.service;

import com.equisibe.model.Contact;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);

    private EmailService service(boolean enabled) {
        return new EmailService(
                sender, enabled, "equisibe@gmail.com", "equisibe@gmail.com");
    }

    private Contact contact() {
        return new Contact("Andrea", "andrea@example.com", "Mi consulta");
    }

    @Test
    void shouldSendToEquisibeAndReplyToCustomer() {
        service(true).sendContact(contact());

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage mail = captor.getValue();

        assertEquals("equisibe@gmail.com", mail.getFrom());
        assertArrayEquals(new String[]{"equisibe@gmail.com"}, mail.getTo());
        assertEquals("andrea@example.com", mail.getReplyTo());
        assertTrue(mail.getText().contains("Mi consulta"));
        assertTrue(mail.getText().contains("Andrea"));
    }

    @Test
    void shouldRejectWhenDisabled() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service(false).sendContact(contact()));
        assertEquals(503, error.getStatusCode().value());
        verifyNoInteractions(sender);
    }

    @Test
    void shouldReportDeliveryFailure() {
        doThrow(new MailSendException("SMTP failure"))
                .when(sender).send(any(SimpleMailMessage.class));

        var error = assertThrows(ResponseStatusException.class,
                () -> service(true).sendContact(contact()));
        assertEquals(503, error.getStatusCode().value());
    }

    @Test
    void shouldRejectHeaderInjection() {
        Contact contact = new Contact(
                "Andrea", "andrea@example.com\r\nBcc: other@example.com", "Consulta");

        var error = assertThrows(ResponseStatusException.class,
                () -> service(true).sendContact(contact));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(sender);
    }
}
