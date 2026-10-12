package com.equisibe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "shipping_addresses")
public class ShippingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 25)
    private String phone;

    @Column(nullable = false, length = 200)
    private String addressLine1;

    @Column(length = 100)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 2)
    private String countryCode;

    protected ShippingAddress() {
    }

    public ShippingAddress(
            User user,
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String postalCode,
            String countryCode) {

        this.user = Objects.requireNonNull(
                user,
                "El usuario propietario es obligatorio."
        );

        updateDetails(
                recipientName,
                phone,
                addressLine1,
                addressLine2,
                city,
                province,
                postalCode,
                countryCode
        );
    }

    public void updateDetails(
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String postalCode,
            String countryCode) {

        this.recipientName = recipientName.strip();
        this.phone = phone.strip();
        this.addressLine1 = addressLine1.strip();
        this.addressLine2 = normalizeOptional(addressLine2);
        this.city = city.strip();
        this.province = normalizeOptional(province);
        this.postalCode = postalCode.strip();
        this.countryCode = countryCode.strip();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }
}