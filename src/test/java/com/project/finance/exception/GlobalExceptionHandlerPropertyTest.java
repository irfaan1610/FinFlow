package com.project.finance.exception;

import net.jqwik.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for GlobalExceptionHandler.
 *
 * Feature: finance-dashboard
 * Property 12: Error responses are structured and safe
 * Validates: Requirements 7.1, 7.4, 7.6
 */
class GlobalExceptionHandlerPropertyTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // -------------------------------------------------------------------------
    // Property 12: Error responses are structured and safe
    // For any exception thrown during request processing, the HTTP response body
    // SHALL be a JSON object containing `timestamp`, `status`, and `message`
    // fields, and SHALL NOT contain Java stack trace text.
    // Validates: Requirements 7.1, 7.4, 7.6
    // -------------------------------------------------------------------------

    @Property(tries = 200)
    @Label("Property 12: ResourceNotFoundException responses are structured and safe")
    void resourceNotFoundResponseIsStructuredAndSafe(
            @ForAll("nonBlankMessages") String message) {

        // Feature: finance-dashboard, Property 12: Error responses are structured and safe
        // Validates: Requirements 7.1, 7.4, 7.6
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new ResourceNotFoundException(message));

        assertStructuredAndSafe(response);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Property(tries = 200)
    @Label("Property 12: DuplicateEmailException responses are structured and safe")
    void duplicateEmailResponseIsStructuredAndSafe(
            @ForAll("nonBlankMessages") String message) {

        // Feature: finance-dashboard, Property 12: Error responses are structured and safe
        // Validates: Requirements 7.1, 7.4, 7.6
        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicateEmail(new DuplicateEmailException(message));

        assertStructuredAndSafe(response);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Property(tries = 200)
    @Label("Property 12: InactiveUserException responses are structured and safe")
    void inactiveUserResponseIsStructuredAndSafe(
            @ForAll("nonBlankMessages") String message) {

        // Feature: finance-dashboard, Property 12: Error responses are structured and safe
        // Validates: Requirements 7.1, 7.4, 7.6
        ResponseEntity<Map<String, Object>> response =
                handler.handleInactiveUser(new InactiveUserException(message));

        assertStructuredAndSafe(response);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Property(tries = 200)
    @Label("Property 12: AccessDeniedException responses are structured and safe")
    void accessDeniedResponseIsStructuredAndSafe(
            @ForAll("nonBlankMessages") String message) {

        // Feature: finance-dashboard, Property 12: Error responses are structured and safe
        // Validates: Requirements 7.1, 7.4, 7.6
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException(message));

        assertStructuredAndSafe(response);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Property(tries = 200)
    @Label("Property 12: Generic Exception responses are structured and safe")
    void genericExceptionResponseIsStructuredAndSafe(
            @ForAll("nonBlankMessages") String message) {

        // Feature: finance-dashboard, Property 12: Error responses are structured and safe
        // Validates: Requirements 7.1, 7.4, 7.6
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new RuntimeException(message));

        assertStructuredAndSafe(response);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        // Generic handler must NOT leak the original exception message (could contain sensitive info)
        String responseMessage = (String) response.getBody().get("message");
        assertThat(responseMessage).isEqualTo("An unexpected error occurred");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void assertStructuredAndSafe(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();

        // Must contain required fields: timestamp, status, message
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsKey("status");
        assertThat(body).containsKey("message");

        // timestamp must be a non-null string
        assertThat(body.get("timestamp")).isNotNull();
        assertThat(body.get("timestamp").toString()).isNotBlank();

        // status must be a valid HTTP status integer
        assertThat(body.get("status")).isInstanceOf(Integer.class);
        int status = (Integer) body.get("status");
        assertThat(status).isBetween(400, 599);

        // message must be present and non-null
        assertThat(body.get("message")).isNotNull();

        // Must NOT contain stack trace indicators
        String bodyString = body.toString();
        assertThat(bodyString).doesNotContain("at com.");
        assertThat(bodyString).doesNotContain("at java.");
        assertThat(bodyString).doesNotContain("Exception in thread");
        assertThat(bodyString).doesNotContain("StackTrace");
        assertThat(bodyString).doesNotContain("\tat ");
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> nonBlankMessages() {
        // Generate realistic error message strings
        // Exclude stack-trace-like patterns since the property guards against
        // actual Java stack traces being exposed, not user messages that look like them
        return Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
                Arbitraries.of(
                        "User not found",
                        "Email already exists",
                        "Account is inactive",
                        "Invalid credentials provided",
                        "Resource with id 999 not found",
                        "Operation not permitted"
                )
        );
    }
}
