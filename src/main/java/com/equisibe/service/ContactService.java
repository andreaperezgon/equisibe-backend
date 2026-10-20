package com.equisibe.service;

import com.equisibe.model.Contact;
import com.equisibe.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final EmailService emailService;

    public ContactService(
            ContactRepository contactRepository,
            EmailService emailService) {
        this.contactRepository = contactRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Contact saveContact(Contact contact) {
        Contact saved = contactRepository.saveAndFlush(contact);
        emailService.sendContact(saved);
        return saved;
    }
}
