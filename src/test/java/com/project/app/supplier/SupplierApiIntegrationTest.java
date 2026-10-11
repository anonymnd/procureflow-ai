package com.project.app.supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.project.app.support.Slice0DatabaseGuard;
import jakarta.persistence.EntityManager;

/** TC-S0-003..008 -> REQ-S0-002..006@1, BL-S0-R1. Fixtures roll back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
@Transactional
class SupplierApiIntegrationTest {
    private static final String API = "/api/suppliers";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void freshEmptyList() throws Exception {
        assertEquals(0, count());
        mvc.perform(get(API)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void completeCrudPersistsUpdateAndDelete() throws Exception {
        long id = create("Atlas Supplies");
        mvc.perform(get(API + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.name").value("Atlas Supplies"));
        mvc.perform(get(API)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body("Updated Supplies")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertEquals("Updated Supplies", name(id));
        mvc.perform(get(API + "/" + id)).andExpect(jsonPath("$.name").value("Updated Supplies"));
        mvc.perform(delete(API + "/" + id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        assertEquals(0, count());
        mvc.perform(get(API + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete(API + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicatesAndUntrimmedNamesRemainAllowed() throws Exception {
        long first = create("  Atlas Supplies  ");
        long second = create("  Atlas Supplies  ");
        assertNotEquals(first, second);
        assertEquals("  Atlas Supplies  ", name(first));
        assertEquals("  Atlas Supplies  ", name(second));
        assertEquals(2, count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void validNameBoundariesPersist(String method) throws Exception {
        long fixture = create("Fixture");
        for (int length : List.of(1, 255)) {
            String name = "x".repeat(length);
            if (method.equals("POST")) assertEquals(name, name(create(name)));
            else {
                mvc.perform(put(API + "/" + fixture).contentType(MediaType.APPLICATION_JSON).content(body(name)))
                        .andExpect(status().isOk());
                assertEquals(name, name(fixture));
                mvc.perform(get(API + "/" + fixture)).andExpect(jsonPath("$.name").value(name));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void invalidNamesAndJsonLeaveFixtureUnchanged(String method) throws Exception {
        long id = create("Unchanged fixture");
        String path = method.equals("POST") ? API : API + "/" + id;
        for (String invalid : List.of("{}", "{\"name\":null}", body(""), body("   "), body("x".repeat(256)))) {
            rejected(request(method, path, invalid), 400, "VALIDATION_ERROR", id);
        }
        rejected(request(method, path, "{\"name\":"), 400, "BAD_REQUEST", id);
        rejected(request(method, path, ""), 400, "BAD_REQUEST", id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingAndBadIdsLeaveFixtureUnchanged(String method) throws Exception {
        long fixture = create("Unchanged fixture");
        for (String absent : List.of("9223372036854775807", "-1")) {
            rejected(request(method, API + "/" + absent, body("Valid")), 404, "Resource Not Found", fixture);
        }
        for (String invalid : List.of("not-a-number", "9223372036854775808")) {
            rejected(request(method, API + "/" + invalid, body("Valid")), 400, "BAD_REQUEST", fixture);
        }
    }

    private void rejected(MockHttpServletRequestBuilder request, int expectedStatus, String error, long fixture) throws Exception {
        int beforeCount = count();
        String beforeName = name(fixture);
        mvc.perform(request).andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus)).andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.timestamp").isNotEmpty()).andExpect(jsonPath("$.message").isNotEmpty());
        assertEquals(beforeCount, count());
        assertEquals(beforeName, name(fixture));
        mvc.perform(get(API + "/" + fixture)).andExpect(jsonPath("$.name").value(beforeName));
    }

    private long create(String name) throws Exception {
        String json = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(name))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private int count() {
        flushPersistenceContext();
        return jdbc.queryForObject("SELECT count(*) FROM suppliers", Integer.class);
    }

    private String name(long id) {
        flushPersistenceContext();
        return jdbc.queryForObject("SELECT name FROM suppliers WHERE id=?", String.class, id);
    }

    private void flushPersistenceContext() {
        // MockMvc joins the rollback transaction. JDBC sees SQL only after JPA flushes.
        entityManager.flush();
        entityManager.clear();
    }
    private String body(String name) { return "{\"name\":\"" + name + "\"}"; }

    private MockHttpServletRequestBuilder request(String method, String path, String body) {
        return switch (method) {
            case "POST" -> post(path).contentType(MediaType.APPLICATION_JSON).content(body);
            case "PUT" -> put(path).contentType(MediaType.APPLICATION_JSON).content(body);
            case "DELETE" -> delete(path);
            default -> get(path);
        };
    }
}
