package com.equisibe.model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
@Entity
public class Contact {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String name;
private String email;
private String message;
}
public Contact() {
}

public Contact(String name, String email, String message) {
    this.name = name;
    this.email = email;
    this.message = message;
}