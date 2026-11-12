package com.sara.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.User;
import com.sara.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSeeder dataSeeder;

    @BeforeEach
    void resetUsers() {
        userRepository.deleteAll();
        dataSeeder.run();
    }

    // ---------- Helpers ----------

    private void registerStudent() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Layla Ahmad","email":"layla@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    // ---------- Registration (US1) ----------

    @Test
    void register_createsStudentWithoutReturningPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Layla Ahmad","email":"layla@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("layla@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_storesPasswordHash() throws Exception {
        registerStudent();

        User saved = userRepository.findByEmail("layla@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_withExistingEmail_returnsConflict() throws Exception {
        registerStudent();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Another Layla","email":"LAYLA@example.com","password":"secret456"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Layla Ahmad","email":"not-an-email","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ---------- Login (US1) ----------

    @Test
    void login_withCorrectDetails_returnsToken() throws Exception {
        registerStudent();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"layla@example.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        registerStudent();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"layla@example.com","password":"wrong1234"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Token checks and roles (US2) ----------

    @Test
    void me_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        registerStudent();
        String token = loginAndGetToken("layla@example.com", "secret123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("layla@example.com"));
    }

    @Test
    void me_withTamperedToken_returnsUnauthorized() throws Exception {
        registerStudent();
        String token = loginAndGetToken("layla@example.com", "secret123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminServicesEndpoint_asStudent_returnsForbidden() throws Exception {
        registerStudent();
        String token = loginAndGetToken("layla@example.com", "secret123");

        mockMvc.perform(get("/api/admin/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminServicesEndpoint_asAdmin_returnsOk() throws Exception {
        String token = loginAndGetToken("admin@dor.com", "admin12345");

        // The admin services endpoint exists now, so an admin gets 200 (not 401 and not 403).
        mockMvc.perform(get("/api/admin/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
