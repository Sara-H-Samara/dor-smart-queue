package com.sara.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.config.DataSeeder;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceTypeControllerTest {

    private static final String URL = "/api/admin/services";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private DataSeeder dataSeeder;

    @BeforeEach
    void reset() {
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
    }

    // ---------- Helpers ----------

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        return loginAndGetToken("admin@dor.com", "admin12345");
    }

    private String studentToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Layla Ahmad","email":"layla@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated());
        return loginAndGetToken("layla@example.com", "secret123");
    }

    private String json(String name) {
        return "{\"name\":\"%s\"}".formatted(name);
    }

    /** Creates a record as admin and returns its id. */
    private long create(String token, String name) throws Exception {
        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // ---------- Admin: list, create, rename, toggle ----------

    @Test
    void admin_canListServices() throws Exception {
        String token = adminToken();
        create(token, "Fees");
        create(token, "Admissions");

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Admissions"))
                .andExpect(jsonPath("$[1].name").value("Fees"));
    }

    @Test
    void admin_canCreateService() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Fees"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void admin_canRenameService() throws Exception {
        String token = adminToken();
        long id = create(token, "Fees");

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Payments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Payments"));
    }

    @Test
    void admin_canToggleActive() throws Exception {
        String token = adminToken();
        long id = create(token, "Fees");

        mockMvc.perform(patch(URL + "/" + id + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch(URL + "/" + id + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---------- Access control ----------

    @Test
    void student_getsForbidden() throws Exception {
        String token = studentToken();

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(URL + "/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(URL + "/1/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_getsUnauthorized() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(URL + "/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Duplicates ----------

    @Test
    void create_withDuplicateName_returnsConflict() throws Exception {
        String token = adminToken();
        create(token, "Fees");

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withDuplicateNameInDifferentCase_returnsConflict() throws Exception {
        String token = adminToken();
        create(token, "Fees");

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("fees")))
                .andExpect(status().isConflict());
    }

    @Test
    void rename_toItsOwnCurrentName_works() throws Exception {
        String token = adminToken();
        long id = create(token, "Fees");

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fees"));
    }

    @Test
    void rename_toAnotherRecordsName_returnsConflict() throws Exception {
        String token = adminToken();
        create(token, "Fees");
        long otherId = create(token, "Admissions");

        mockMvc.perform(put(URL + "/" + otherId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("fees")))
                .andExpect(status().isConflict());
    }

    // ---------- Unknown id ----------

    @Test
    void rename_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(put(URL + "/999999")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fees")))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggle_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(patch(URL + "/999999/active")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    // ---------- Validation ----------

    @Test
    void create_withBlankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withTooLongName_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("a".repeat(101))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rename_withBlankName_returnsBadRequest() throws Exception {
        String token = adminToken();
        long id = create(token, "Fees");

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rename_withTooLongName_returnsBadRequest() throws Exception {
        String token = adminToken();
        long id = create(token, "Fees");

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("a".repeat(101))))
                .andExpect(status().isBadRequest());
    }

    // ---------- Deactivating keeps the row ----------

    @Test
    void deactivate_keepsRecordInAdminList() throws Exception {
        String token = adminToken();
        long id = create(token, "Fees");

        mockMvc.perform(patch(URL + "/" + id + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("Fees"))
                .andExpect(jsonPath("$[0].active").value(false));
    }
}
