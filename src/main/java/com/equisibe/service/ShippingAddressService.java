package com.equisibe.service;

import com.equisibe.dto.ShippingAddressRequest;
import com.equisibe.dto.ShippingAddressResponse;
import com.equisibe.model.ShippingAddress;
import com.equisibe.model.User;
import com.equisibe.repository.ShippingAddressRepository;
import com.equisibe.repository.UserRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ShippingAddressService {

    private final ShippingAddressRepository addressRepository;
    private final UserRepository userRepository;

    public ShippingAddressService(
            ShippingAddressRepository addressRepository,
            UserRepository userRepository) {

        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<ShippingAddressResponse> getAddresses(String email) {
        User user = getActiveUser(email);

        return addressRepository
                .findByUser_IdOrderByIdDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ShippingAddressResponse getAddress(String email, Long id) {
        User user = getActiveUser(email);

        ShippingAddress address = getOwnedAddress(id, user.getId());

        return toResponse(address);
    }

    @Transactional
    public ShippingAddressResponse createAddress(
            String email,
            ShippingAddressRequest request) {

        User user = getActiveUser(email);

        ShippingAddress address = new ShippingAddress(
                user,
                request.recipientName(),
                request.phone(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.province(),
                request.postalCode(),
                request.countryCode()
        );

        ShippingAddress savedAddress = addressRepository.save(address);

        return toResponse(savedAddress);
    }

    @Transactional
    public ShippingAddressResponse updateAddress(
            String email,
            Long id,
            ShippingAddressRequest request) {

        User user = getActiveUser(email);

        ShippingAddress address = getOwnedAddress(id, user.getId());

        address.updateDetails(
                request.recipientName(),
                request.phone(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.province(),
                request.postalCode(),
                request.countryCode()
        );

        ShippingAddress savedAddress = addressRepository.save(address);

        return toResponse(savedAddress);
    }

    private User getActiveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        return userRepository.findByEmail(email)
                .filter(account -> account.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "La sesión no es válida."
                ));
    }

    private ShippingAddress getOwnedAddress(Long id, Long userId) {
        return addressRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La dirección no está disponible."
                ));
    }

    private ShippingAddressResponse toResponse(
            ShippingAddress address) {

        return new ShippingAddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getCountryCode()
        );
    }
}