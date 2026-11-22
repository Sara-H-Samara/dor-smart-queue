package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.User;
import com.sara.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Registration, login and roles")
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

    @Test
    @DisplayName("Registering a student never returns the password")
    void register_withValidData_returnsStudentWithoutPassword() throws Exception {
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
    @DisplayName("A new password is stored as a hash, not as plain text")
    void register_withValidData_storesPasswordHash() throws Exception {
        registerStudent();

        User saved = userRepository.findByEmail("layla@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("A registered email cannot be used again, even in other letter case")
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
    @DisplayName("Registration is rejected when the email is not valid")
    void register_withInvalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Layla Ahmad","email":"not-an-email","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A student with correct details logs in and receives a token")
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
    @DisplayName("Login fails with an incorrect password")
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        registerStudent();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"layla@example.com","password":"wrong1234"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token that was changed is not accepted")
    void listActiveServices_withTamperedToken_returnsUnauthorized() throws Exception {
        registerStudent();
        String token = loginAndGetToken("layla@example.com", "secret123");

        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A student cannot open the admin services list")
    void adminServicesEndpoint_asStudent_returnsForbidden() throws Exception {
        registerStudent();
        String token = loginAndGetToken("layla@example.com", "secret123");

        mockMvc.perform(get("/api/admin/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin can open the admin services list")
    void adminServicesEndpoint_asAdmin_returnsOk() throws Exception {
        String token = loginAndGetToken("admin@dor.com", "admin12345");

        mockMvc.perform(get("/api/admin/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
