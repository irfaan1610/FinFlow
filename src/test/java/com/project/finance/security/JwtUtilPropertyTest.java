package com.project.finance.security;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for JwtUtil.
 *
 * Feature: finance-dashboard
 * Property 2: JWT round-trip preserves identity
 * Property 4: Invalid JWT tokens are always rejected
 */
class JwtUtilPropertyTest {

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 86_400_000L; // 24h

    private JwtUtil buildJwtUtil(long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(util, "expirationMs", expirationMs);
        return util;
    }

    private UserDetails userDetails(String email, String role) {
        return new User(email, "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    // -------------------------------------------------------------------------
    // Property 2: JWT round-trip preserves identity
    // Validates: Requirements 1.3
    // -------------------------------------------------------------------------

    @Property(tries = 200)
    @Label("Property 2: JWT round-trip preserves identity")
    void jwtRoundTripPreservesEmail(
            @ForAll("validEmails") String email,
            @ForAll("roles") String role) {

        // Feature: finance-dashboard, Property 2: JWT round-trip preserves identity
        // Validates: Requirements 1.3
        JwtUtil jwtUtil = buildJwtUtil(EXPIRATION_MS);
        UserDetails user = userDetails(email, role);

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();
    }

    // -------------------------------------------------------------------------
    // Property 4: Invalid JWT tokens are always rejected
    // Validates: Requirements 2.4, 2.6
    // -------------------------------------------------------------------------

    @Property(tries = 200)
    @Label("Property 4: Tampered tokens are always rejected")
    void tamperedTokenIsAlwaysRejected(
            @ForAll("validEmails") String email,
            @ForAll("roles") String role,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String suffix) {

        // Feature: finance-dashboard, Property 4: Invalid JWT tokens are always rejected
        // Validates: Requirements 2.4, 2.6
        JwtUtil jwtUtil = buildJwtUtil(EXPIRATION_MS);
        UserDetails user = userDetails(email, role);

        String token = jwtUtil.generateToken(user);
        String tampered = token + suffix;

        assertThat(jwtUtil.isTokenValid(tampered, user)).isFalse();
    }

    @Property(tries = 200)
    @Label("Property 4: Expired tokens are always rejected")
    void expiredTokenIsAlwaysRejected(
            @ForAll("validEmails") String email,
            @ForAll("roles") String role) {

        // Feature: finance-dashboard, Property 4: Invalid JWT tokens are always rejected
        // Validates: Requirements 2.4, 2.6
        JwtUtil jwtUtil = buildJwtUtil(1L); // 1ms expiry — token expires immediately
        UserDetails user = userDetails(email, role);

        String token = jwtUtil.generateToken(user);

        // Small sleep to ensure expiry
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(jwtUtil.isTokenValid(token, user)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validEmails() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
                .flatMap(local -> Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(6)
                        .map(domain -> local + "@" + domain + ".com"));
    }

    @Provide
    Arbitrary<String> roles() {
        return Arbitraries.of("VIEWER", "ANALYST", "ADMIN");
    }
}
