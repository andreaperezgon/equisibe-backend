package com.equisibe.service;

import com.equisibe.model.Contact;
import com.equisibe.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void shouldSaveContact() {

        Contact contact = new Contact(
                "Andrea",
                "andrea@email.com",
                "Quiero información"
        );

        when(contactRepository.save(contact)).thenReturn(contact);

        Contact savedContact = contactService.saveContact(contact);

        assertEquals("Andrea", savedContact.getName());
        assertEquals("andrea@email.com", savedContact.getEmail());
        assertEquals("Quiero información", savedContact.getMessage());

        verify(contactRepository).save(contact);
    }
}