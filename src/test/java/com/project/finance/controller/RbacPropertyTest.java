package com.project.finance.controller;

import com.project.finance.model.Role;
import com.project.finance.model.User;
import com.project.finance.model.UserStatus;
import com.project.finance.repository.UserRepository;
import com.project.finance.security.JwtUtil;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Property-based test for RBAC enforcement.
 *
 * Feature: finance-dashboard
 * Property 3: RBAC enforcement is consistent
 * Validates: Requirements 2.1, 2.2, 2.3, 2.5
 *
 * For any authenticated request, the HTTP response status SHALL be 403 if and
 * only if the authenticated user's role does not have permission for the
 * requested endpoint, and NOT 403 if the role does have permission.
 */
@SpringBootTest
@AutoConfigureMockMvc
@JqwikSpringSupport
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /** HTTP method as a simple string to stay compatible with Java 17. */
    record Endpoint(String method, String path, Set<Role> allowedRoles, String body) {}

    // Valid request bodies for write endpoints — ensures @Valid passes so that
    // Spring Security's @PreAuthorize can run and return 403 (not 400).
    private static final String VALID_RECORD_BODY =
            "{\"amount\":100.00,\"type\":\"INCOME\",\"category\":\"Salary\",\"date\":\"2026-01-15\"}";
    private static final String VALID_ROLE_BODY   = "{\"role\":\"ANALYST\"}";
    private static final String VALID_STATUS_BODY = "{\"status\":\"ACTIVE\"}";

    /**
     * All protected endpoints and their allowed roles, derived from the controllers.
     * Public endpoints (/auth/**, /swagger-ui/**, /v3/api-docs/**) are excluded.
     */
    private static final List<Endpoint> PROTECTED_ENDPOINTS = List.of(
        // Dashboard — VIEWER, ANALYST, ADMIN
        new Endpoint("GET",    "/dashboard/summary", Set.of(Role.VIEWER, Role.ANALYST, Role.ADMIN), ""),
        new Endpoint("GET",    "/dashboard/trends",  Set.of(Role.VIEWER, Role.ANALYST, Role.ADMIN), ""),
        // Records read — ANALYST, ADMIN
        new Endpoint("GET",    "/records",           Set.of(Role.ANALYST, Role.ADMIN), ""),
        // Records write — ADMIN only (valid bodies so @Valid doesn't short-circuit to 400)
        new Endpoint("POST",   "/records",           Set.of(Role.ADMIN), VALID_RECORD_BODY),
        new Endpoint("PUT",    "/records/1",         Set.of(Role.ADMIN), VALID_RECORD_BODY),
        new Endpoint("DELETE", "/records/1",         Set.of(Role.ADMIN), ""),
        // Users — ADMIN only
        new Endpoint("GET",    "/users",             Set.of(Role.ADMIN), ""),
        new Endpoint("PUT",    "/users/1/role",      Set.of(Role.ADMIN), VALID_ROLE_BODY),
        new Endpoint("PATCH",  "/users/1/status",    Set.of(Role.ADMIN), VALID_STATUS_BODY)
    );

    // -------------------------------------------------------------------------
    // Property 3: RBAC enforcement is consistent
    // For any (role, endpoint) combination, the response is 403 iff the role
    // is not in the endpoint's allowed set, and NOT 403 otherwise.
    // Validates: Requirements 2.1, 2.2, 2.3, 2.5
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 3: RBAC enforcement is consistent")
    void rbacEnforcementIsConsistent(
            @ForAll("roles") Role role,
            @ForAll("endpointIndices") int endpointIndex) throws Exception {

        // Feature: finance-dashboard, Property 3: RBAC enforcement is consistent
        // Validates: Requirements 2.1, 2.2, 2.3, 2.5

        Endpoint endpoint = PROTECTED_ENDPOINTS.get(endpointIndex);
        String token = generateTokenForRole(role);

        MockHttpServletRequestBuilder request = buildRequest(endpoint)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json");

        MvcResult result = mockMvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();

        boolean roleIsAllowed = endpoint.allowedRoles().contains(role);

        if (roleIsAllowed) {
            // Role has permission — must NOT receive 403
            assertThat(status)
                    .as("Role %s should be allowed on %s %s but got 403",
                            role, endpoint.method(), endpoint.path())
                    .isNotEqualTo(403);
        } else {
            // Role does not have permission — must receive 403
            assertThat(status)
                    .as("Role %s should be forbidden on %s %s but got %d",
                            role, endpoint.method(), endpoint.path(), status)
                    .isEqualTo(403);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String generateTokenForRole(Role role) {
        String email = "rbac-test-" + role.name().toLowerCase() + "@test.com";

        // Ensure the user exists in the DB so UserDetailsService can load it
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setName("RBAC Test " + role.name());
            user.setEmail(email);
            // Pre-encoded bcrypt hash for "password"
            user.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email,
                "irrelevant",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        return jwtUtil.generateToken(userDetails);
    }

    private MockHttpServletRequestBuilder buildRequest(Endpoint endpoint) {
        String path = endpoint.path();
        String body = endpoint.body();
        switch (endpoint.method()) {
            case "GET":    return get(path);
            case "POST":   return post(path).content(body);
            case "PUT":    return put(path).content(body);
            case "PATCH":  return patch(path).content(body);
            case "DELETE": return delete(path);
            default: throw new IllegalArgumentException("Unsupported method: " + endpoint.method());
        }
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.class);
    }

    @Provide
    Arbitrary<Integer> endpointIndices() {
        return Arbitraries.integers().between(0, PROTECTED_ENDPOINTS.size() - 1);
    }
}
