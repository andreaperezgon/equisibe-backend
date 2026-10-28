package com.equisibe.service;

import com.equisibe.model.Contact;
import com.equisibe.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock ContactRepository contactRepository;
    @Mock EmailService emailService;
    @InjectMocks ContactService contactService;

    @Test
    void shouldSaveAndSendContact() {
        Contact contact = new Contact(
                "Andrea", "andrea@example.com", "Quiero información");
        when(contactRepository.saveAndFlush(contact)).thenReturn(contact);

        assertSame(contact, contactService.saveContact(contact));

        verify(contactRepository).saveAndFlush(contact);
        verify(emailService).sendContact(contact);
    }

    @Test
    void shouldReportEmailFailure() {
        Contact contact = new Contact(
                "Andrea", "andrea@example.com", "Consulta");
        when(contactRepository.saveAndFlush(contact)).thenReturn(contact);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .when(emailService).sendContact(contact);

        assertThrows(ResponseStatusException.class,
                () -> contactService.saveContact(contact));
    }
}
