package com.equisibe.controller;

import com.equisibe.exception.ApiExceptionHandler;
import com.equisibe.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.equisibe.dto.UserResponse;
import com.equisibe.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.equisibe.exception.EmailAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
    @Test
void shouldReturnCreatedUserWithoutPassword() throws Exception {
    var response = new UserResponse(
            1L, "Andrea", "andrea@example.com", Role.CUSTOMER
    );
    when(authService.register(any())).thenReturn(response);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "name": "Andrea",
                      "email": "andrea@example.com",
                      "password": "ExamplePassword123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("andrea@example.com"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.password").doesNotExist());
}
@Test
void shouldReturnConflictForExistingEmail() throws Exception {
    when(authService.register(any()))
            .thenThrow(new EmailAlreadyExistsException());

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "name": "Andrea",
                      "email": "andrea@example.com",
                      "password": "ExamplePassword123"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail")
                    .value("El correo electrónico ya está registrado."));
}
@Test
void shouldRejectInvalidRegistrationData() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "name": "",
                      "email": "invalid-email",
                      "password": "123"
                    }
                    """))
            .andExpect(status().isBadRequest());

    verifyNoInteractions(authService);
}
}