package com.equisibe.controller;

import com.equisibe.model.Contact;
import com.equisibe.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController contactController;

    @Test
    void shouldCreateContact() {

        Contact contact = new Contact(
                "Andrea",
                "andrea@email.com",
                "Quiero información"
        );

        when(contactService.saveContact(contact)).thenReturn(contact);

        ResponseEntity<Contact> response =
                contactController.createContact(contact);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(contact, response.getBody());

        verify(contactService).saveContact(contact);
    }
}