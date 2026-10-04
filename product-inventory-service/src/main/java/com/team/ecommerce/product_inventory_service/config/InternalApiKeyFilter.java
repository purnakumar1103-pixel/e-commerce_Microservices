package com.team.ecommerce.product_inventory_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private final byte[] expectedKey;
    private final ApiErrorWriter errorWriter;

    public InternalApiKeyFilter(@Value("${app.internal-api-key}") String expectedKey,
                                ApiErrorWriter errorWriter) {
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER);
        byte[] providedBytes = providedKey == null ? new byte[0] : providedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedKey, providedBytes)) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                    "A valid internal API key is required", java.util.Map.of());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
