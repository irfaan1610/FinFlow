package com.project.finance.service;

import com.project.finance.dto.response.UserResponse;
import com.project.finance.model.Role;
import com.project.finance.model.User;
import com.project.finance.model.UserStatus;
import com.project.finance.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for UserService.
 *
 * Feature: finance-dashboard
 * Property 6: Role update round-trip
 * Validates: Requirements 3.2
 */
@SpringBootTest
@JqwikSpringSupport
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserServicePropertyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Property 6: Role update round-trip
    // For any existing user and any valid Role value, updating the user's role
    // via updateRole and then fetching the user SHALL return the updated role.
    // Validates: Requirements 3.2
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 6: Role update round-trip")
    void roleUpdateRoundTrip(@ForAll("existingUsers") User user, @ForAll("anyRole") Role newRole) {
        // Feature: finance-dashboard, Property 6: Role update round-trip
        // Validates: Requirements 3.2

        User saved = userRepository.save(user);
        try {
            UserResponse response = userService.updateRole(saved.getId(), newRole);

            // Response must reflect the new role
            assertThat(response.getRole()).isEqualTo(newRole);

            // Fetching from DB must also reflect the new role
            User reloaded = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getRole()).isEqualTo(newRole);
        } finally {
            userRepository.deleteById(saved.getId());
        }
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<User> existingUsers() {
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20);
        Arbitrary<String> localParts = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        Arbitrary<String> domains = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(6);
        Arbitrary<Role> roles = Arbitraries.of(Role.class);

        return Combinators.combine(names, localParts, domains, roles)
                .as((name, local, domain, role) -> {
                    User u = new User();
                    u.setName(name);
                    u.setEmail(local + "@" + domain + ".com");
                    // Use a pre-encoded bcrypt hash to avoid calling passwordEncoder in the provider
                    u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
                    u.setRole(role);
                    u.setStatus(UserStatus.ACTIVE);
                    return u;
                });
    }

    @Provide
    Arbitrary<Role> anyRole() {
        return Arbitraries.of(Role.class);
    }
}
