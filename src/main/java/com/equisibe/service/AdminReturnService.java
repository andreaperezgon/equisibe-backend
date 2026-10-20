package com.equisibe.service;

import com.equisibe.dto.ReturnResponse;
import com.equisibe.model.ReturnRequest;
import com.equisibe.model.ReturnStatus;
import com.equisibe.model.Role;
import com.equisibe.repository.ReturnRequestRepository;
import com.equisibe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminReturnService {
    private final ReturnRequestRepository requests;
    private final UserRepository users;
    private final EntityManager entityManager;

    public AdminReturnService(
            ReturnRequestRepository requests,
            UserRepository users,
            EntityManager entityManager) {
        this.requests = requests;
        this.users = users;
        this.entityManager = entityManager;
    }

    public List<AdminReturn> list(String email) {
        requireAdmin(email);
        return requests.findAllByOrderByCreatedAtDescIdDesc()
                .stream().map(AdminReturn::from).toList();
    }

    @Transactional
    public AdminReturn update(
            String email, Long id, ReturnStatus status, String note) {
        requireAdmin(email);
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        var request = requests.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND));

        entityManager.lock(
                request.getOrder(), LockModeType.PESSIMISTIC_WRITE
        );
        entityManager.refresh(
                request, LockModeType.PESSIMISTIC_WRITE
        );

        try {
            request.updateStatus(status, note);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, failure.getMessage()
            );
        }

        entityManager.flush();
        return AdminReturn.from(request);
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

    public record AdminReturn(
            ReturnResponse request,
            String customerName,
            String customerEmail
    ) {
        public static AdminReturn from(ReturnRequest request) {
            var customer = request.getOrder().getUser();
            return new AdminReturn(
                    ReturnResponse.from(request),
                    customer.getName(), customer.getEmail()
            );
        }
    }
}
