package com.equisibe.service;

import com.equisibe.model.Contact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailService {

    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
    private final String contactTo;

    public EmailService(
            JavaMailSender sender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.contact-to:equisibe@gmail.com}") String contactTo) {
        this.sender = sender;
        this.enabled = enabled;
        this.from = from;
        this.contactTo = contactTo;
    }

    public void sendContact(Contact contact) {
        if (!enabled || from.isBlank() || contactTo.isBlank()) {
            throw unavailable();
        }

        String replyTo = contact.getEmail();
        if (replyTo == null || replyTo.contains("\r") || replyTo.contains("\n")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El correo no es válido.");
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(contactTo);
        mail.setReplyTo(replyTo);
        mail.setSubject("Nueva consulta desde la web de Equisibé");
        mail.setText(
                "Nombre: " + contact.getName()
                + "\nCorreo: " + replyTo
                + "\n\nMensaje:\n" + contact.getMessage());

        try {
            sender.send(mail);
        } catch (MailException exception) {
            throw unavailable();
        }
    }


    public void requireAvailable() {
        if (!enabled || from.isBlank()) {
            throw unavailable();
        }
    }

    public void sendPasswordReset(String email, String link) {
        requireAvailable();

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(email);
        mail.setSubject("Recupera tu contraseña de Equisibé");
        mail.setText(
                "Has solicitado cambiar tu contraseña de Equisibé."
                + "\n\nAbre este enlace para elegir una nueva contraseña:"
                + "\n" + link
                + "\n\nEl enlace caduca en 30 minutos y solo puede utilizarse una vez."
                + "\nSi no lo has solicitado, puedes ignorar este correo.");

        try {
            sender.send(mail);
        } catch (MailException exception) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo enviar el mensaje. Inténtalo más tarde.");
    }
}
