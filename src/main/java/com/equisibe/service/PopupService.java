package com.equisibe.service;

import com.equisibe.dto.PopupRequest;
import com.equisibe.dto.PopupResponse;
import com.equisibe.model.PromotionalPopup;
import com.equisibe.model.Role;
import com.equisibe.repository.PromotionalPopupRepository;
import com.equisibe.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PopupService {
    private final PromotionalPopupRepository popups;
    private final UserRepository users;
    private final PromotionalCodeService codes;

    public PopupService(
            PromotionalPopupRepository popups,
            UserRepository users,
            PromotionalCodeService codes) {
        this.popups = popups;
        this.users = users;
        this.codes = codes;
    }

    public List<PopupResponse> getAll(String email) {
        requireAdmin(email);
        return popups.findAllByOrderByUpdatedAtDescIdDesc()
                .stream().map(PopupResponse::from).toList();
    }

    public Optional<PopupResponse> getActive() {
        return popups.findFirstByActiveTrueOrderByUpdatedAtDescIdDesc()
                .map(PopupResponse::from);
    }

    @Transactional
    public PopupResponse create(String email, PopupRequest request) {
        requireAdmin(email);
        return PopupResponse.from(popups.saveAndFlush(newPopup(request)));
    }

    @Transactional
    public PopupResponse update(String email, Long id, PopupRequest request) {
        requireAdmin(email);
        String code = validateCode(request.promoCode());
        var popup = find(id);
        popup.updateDetails(request.title(), request.text(), "", "", "");
        popup.setPromoCode(code);
        return PopupResponse.from(popups.saveAndFlush(popup));
    }

    @Transactional
    public PopupResponse setActive(String email, Long id, boolean active) {
        requireAdmin(email);
        var popup = find(id);
        if (active) validateCode(popup.getPromoCode());
        popup.setActive(active);
        return PopupResponse.from(popups.saveAndFlush(popup));
    }

    @Transactional
    public void delete(String email, Long id) {
        requireAdmin(email);
        popups.delete(find(id));
    }

    @Transactional
    public List<PopupResponse> createExamples(String email) {
        requireAdmin(email);
        example(
                "EQUISIBE10", "10 % DE BIENVENIDA",
                "¡Bienvenido a Equisibé! Usa este código y disfruta de un 10 % de descuento en tus prendas."
        );
        example(
                "EQUISIBE20", "20 % PARA DARTE UN CAPRICHO",
                "Encuentra tu próxima prenda favorita y consigue un 20 % de descuento con este código."
        );
        return getAll(email);
    }

    private void example(String code, String title, String text) {
        if (!popups.existsByPromoCode(code)) {
            popups.saveAndFlush(newPopup(new PopupRequest(title, text, code)));
        }
    }

    private PromotionalPopup newPopup(PopupRequest request) {
        String code = validateCode(request.promoCode());
        var popup = new PromotionalPopup(
                request.title(), request.text(), "", "", ""
        );
        popup.setPromoCode(code);
        return popup;
    }

    private String validateCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        codes.getPercentage(code);
        return code;
    }

    private PromotionalPopup find(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return popups.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El anuncio ya no existe."
                ));
    }

    private void requireAdmin(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        users.findByEmail(email)
                .filter(user -> user.isEnabled() && user.getRole() == Role.ADMIN)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
