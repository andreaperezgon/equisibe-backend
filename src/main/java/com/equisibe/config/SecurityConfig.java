package com.equisibe.config;

import com.equisibe.repository.UserRepository;
import com.equisibe.security.CredentialVersionFilter;
import com.equisibe.security.GoogleLoginHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserRepository users,
            ObjectProvider<ClientRegistrationRepository> registrations,
            ObjectProvider<GoogleLoginHandler> googleHandlers) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .addFilterBefore(
                        new CredentialVersionFilter(users),
                        AuthorizationFilter.class)
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/payments/stripe/webhook"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf",
                                "/api/shop/status",
                                "/api/popups/active",
                                "/api/products",
                                "/api/products/*",
                                "/api/product-images/*",
                                "/oauth2/authorization/google",
                                "/login/oauth2/code/google")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/contacts",
                                "/api/payments/stripe/webhook")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .logout(logout -> logout
                        .logoutRequestMatcher(request ->
                                request.getMethod().equals("POST")
                                && request.getRequestURI()
                                        .equals("/api/auth/logout"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)));

        var googleRegistration = registrations.getIfAvailable();

        if (googleRegistration != null) {
            var handler = googleHandlers.getObject();

            http.oauth2Login(oauth -> oauth
                    .clientRegistrationRepository(googleRegistration)
                    .successHandler(handler)
                    .failureHandler(handler));

            http.exceptionHandling(errors ->
                    errors.defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            request -> request.getRequestURI()
                                    .startsWith("/api/")));
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
