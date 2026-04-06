package com.project.finance.service;

import com.project.finance.dto.request.LoginRequest;
import com.project.finance.dto.request.RegisterRequest;
import com.project.finance.dto.response.AuthResponse;
import com.project.finance.exception.InactiveUserException;
import com.project.finance.model.Role;
import com.project.finance.model.User;
import com.project.finance.model.UserStatus;
import com.project.finance.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for AuthService.
 *
 * Feature: finance-dashboard
 * Property 1: Registration defaults are always correct
 * Property 5: Inactive users cannot authenticate
 * Validates: Requirements 1.1, 3.6
 */
@SpringBootTest
@JqwikSpringSupport
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthServicePropertyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Property 1: Registration defaults are always correct
    // For any valid registration request, the created User SHALL have
    // role=VIEWER, status=ACTIVE, and a bcrypt-hashed password.
    // Validates: Requirements 1.1
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 1: Registration defaults are always correct")
    void registrationDefaultsAreCorrect(@ForAll("validRegisterRequests") RegisterRequest req) {
        // Feature: finance-dashboard, Property 1: Registration defaults are always correct
        // Validates: Requirements 1.1

        // Clean up any prior registration with this email to avoid duplicate conflicts
        userRepository.findByEmail(req.getEmail()).ifPresent(u -> userRepository.delete(u));

        AuthResponse response = authService.register(req);

        // Token must be issued
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo(req.getEmail());

        // Verify persisted user has correct defaults
        User saved = userRepository.findByEmail(req.getEmail()).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.VIEWER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Password must be bcrypt-hashed, not stored as plaintext
        assertThat(saved.getPassword()).isNotEqualTo(req.getPassword());
        assertThat(passwordEncoder.matches(req.getPassword(), saved.getPassword())).isTrue();

        // Clean up after each trial
        userRepository.delete(saved);
    }

    // -------------------------------------------------------------------------
    // Property 5: Inactive users cannot authenticate
    // For any user whose status is INACTIVE, a login attempt with correct
    // credentials SHALL throw InactiveUserException and SHALL NOT issue a token.
    // Validates: Requirements 3.6
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 5: Inactive users cannot authenticate")
    void inactiveUsersCannotAuthenticate(@ForAll("validRegisterRequests") RegisterRequest req) {
        // Feature: finance-dashboard, Property 5: Inactive users cannot authenticate
        // Validates: Requirements 3.6

        // Clean up any prior user with this email
        userRepository.findByEmail(req.getEmail()).ifPresent(u -> userRepository.delete(u));

        // Register the user (starts ACTIVE)
        authService.register(req);

        // Deactivate the user directly in the repository
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        // Attempt login — must be rejected
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(req.getEmail());
        loginRequest.setPassword(req.getPassword());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InactiveUserException.class);

        // Clean up
        userRepository.delete(userRepository.findByEmail(req.getEmail()).orElseThrow());
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<RegisterRequest> validRegisterRequests() {
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20);
        Arbitrary<String> localParts = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        Arbitrary<String> domains = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(6);
        Arbitrary<String> passwords = Arbitraries.strings().ascii()
                .ofMinLength(6).ofMaxLength(20)
                .filter(p -> !p.isBlank());

        return Combinators.combine(names, localParts, domains, passwords)
                .as((name, local, domain, password) -> {
                    RegisterRequest r = new RegisterRequest();
                    r.setName(name);
                    r.setEmail(local + "@" + domain + ".com");
                    r.setPassword(password);
                    return r;
                });
    }
}
