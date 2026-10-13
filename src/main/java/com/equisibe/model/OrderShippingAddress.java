package com.equisibe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderShippingAddress {

    @Column(name = "shipping_recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "shipping_phone", nullable = false, length = 25)
    private String phone;

    @Column(name = "shipping_address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "shipping_address_line2", length = 100)
    private String addressLine2;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;

    @Column(name = "shipping_province", length = 100)
    private String province;

    @Column(name = "shipping_postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "shipping_country_code", nullable = false, length = 2)
    private String countryCode;

    protected OrderShippingAddress() {
    }

    public OrderShippingAddress(
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String postalCode,
            String countryCode) {

        if (!"ES".equals(countryCode)) {
            throw new IllegalArgumentException(
                    "Solo realizamos envíos a España."
            );
        }

        this.recipientName = recipientName;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
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