package com.equisibe.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("El correo electrónico ya está registrado.");
    }
}