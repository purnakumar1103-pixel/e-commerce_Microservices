package com.team.ecommerce.product_inventory_service.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import com.team.ecommerce.product_inventory_service.domain.Category;
import com.team.ecommerce.product_inventory_service.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductInventoryControllerSecurityTests {

    private static final UUID BOOKS_CATEGORY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void seedCategory() {
        categoryRepository.findById(BOOKS_CATEGORY)
                .orElseGet(() -> categoryRepository.save(new Category(BOOKS_CATEGORY, "Books")));
    }

    @Test
    void publicCatalogDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void adminPathWithoutTokenReturnsSharedUnauthorizedError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void customerTokenCannotUseAdminPath() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(jwt().jwt(token -> token.subject("customer-1").claim("role", "CUSTOMER")
                                .issuer("ecommerce-user-service")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void adminTokenCanCreateProduct() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(jwt().jwt(token -> token.subject("admin-1").claim("role", "ADMIN")
                                .issuer("ecommerce-user-service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableQuantity").value(3))
                .andExpect(jsonPath("$.sku").value(org.hamcrest.Matchers.startsWith("TEST-")));
    }

    @Test
    void internalPathRequiresApiKey() throws Exception {
        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderReference\":\"" + UUID.randomUUID()
                                + "\",\"items\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void internalPathRejectsWrongApiKey() throws Exception {
        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderReference\":\"" + UUID.randomUUID()
                                + "\",\"items\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private String productPayload() {
        return "{\"categoryId\":\"" + BOOKS_CATEGORY + "\","
                + "\"sku\":\"TEST-" + UUID.randomUUID() + "\","
                + "\"name\":\"Test Product\",\"description\":\"test\","
                + "\"price\":12.50,\"currency\":\"USD\",\"startingQuantity\":3}";
    }
}
