package com.project.app.supplier.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.project.app.common.exception.ResourceNotFoundException;
import com.project.app.supplier.DTO.SupplierDtoRequest;
import com.project.app.supplier.DTO.SupplierDtoResponse;
import com.project.app.supplier.service.SupplierService;

/** TC-S0-006/007/008/009 -> REQ-S0-004/005@1, BL-S0-R1; no database. */
@WebMvcTest(SupplierController.class)
@Import(GlobalExceptionHandler.class)
class SupplierControllerTest {
    private static final String API = "/api/suppliers";
    @Autowired MockMvc mvc;
    @MockitoBean SupplierService service;

    @Test
    void preservesSuccessfulStatusAndDtoContracts() throws Exception {
        var request = new SupplierDtoRequest("Atlas Supplies");
        var response = new SupplierDtoResponse(1L, "Atlas Supplies");
        when(service.createSupplier(request)).thenReturn(response);
        when(service.getSupplierById(1L)).thenReturn(response);
        when(service.getAllSuppliers()).thenReturn(List.of(response));
        when(service.updateSupplier(1L, request)).thenReturn(response);
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Atlas Supplies\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.name").value("Atlas Supplies"));
        mvc.perform(get(API + "/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Atlas Supplies"));
        mvc.perform(get(API)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(put(API + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Atlas Supplies\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(delete(API + "/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingSupplierIs404(String method) throws Exception {
        String message = "Supplier not found with id: 999";
        when(service.getSupplierById(999L)).thenThrow(new ResourceNotFoundException(message));
        when(service.updateSupplier(eq(999L), any())).thenThrow(new ResourceNotFoundException(message));
        doThrow(new ResourceNotFoundException(message)).when(service).deleteSupplier(999L);
        error(mvc.perform(request(method, API + "/999", "{\"name\":\"Valid\"}")), 404, "Resource Not Found")
                .andExpect(jsonPath("$.message").value(message));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void invalidNamesNeverReachService(String method) throws Exception {
        for (String body : List.of("{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}",
                "{\"name\":\"" + "x".repeat(256) + "\"}")) {
            error(mvc.perform(request(method, path(method), body)), 400, "VALIDATION_ERROR");
        }
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void acceptsOneAnd255CharacterNames(String method) throws Exception {
        for (int length : List.of(1, 255)) {
            String name = "x".repeat(length);
            var request = new SupplierDtoRequest(name);
            var response = new SupplierDtoResponse(1L, name);
            if (method.equals("POST")) when(service.createSupplier(request)).thenReturn(response);
            else when(service.updateSupplier(1L, request)).thenReturn(response);
            mvc.perform(request(method, path(method), "{\"name\":\"" + name + "\"}"))
                    .andExpect(status().is(method.equals("POST") ? 201 : 200)).andExpect(jsonPath("$.name").value(name));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void badOrMissingJsonNeverReachesService(String method) throws Exception {
        error(mvc.perform(request(method, path(method), "{\"name\":")), 400, "BAD_REQUEST");
        error(mvc.perform(request(method, path(method), "")), 400, "BAD_REQUEST");
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void badIdNeverReachesService(String method) throws Exception {
        for (String id : List.of("not-a-number", "9223372036854775808")) {
            error(mvc.perform(request(method, API + "/" + id, "{\"name\":\"Valid\"}")), 400, "BAD_REQUEST");
        }
        verifyNoInteractions(service);
    }

    @Test
    void negativeNumericIdIsLookupRatherThanBadRequest() throws Exception {
        when(service.getSupplierById(-1L)).thenThrow(new ResourceNotFoundException("Supplier not found with id: -1"));
        error(mvc.perform(get(API + "/-1")), 404, "Resource Not Found");
        verify(service).getSupplierById(-1L);
    }

    @Test
    void integrityErrorIsSanitized409() throws Exception {
        when(service.createSupplier(any())).thenThrow(new DataIntegrityViolationException("SENSITIVE_SQL_PASSWORD_SENTINEL"));
        error(mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Valid\"}")), 409, "DATA_CONFLICT")
                .andExpect(jsonPath("$.message").value("The operation conflicts with existing data"))
                .andExpect(content().string(not(containsString("SENSITIVE_SQL_PASSWORD_SENTINEL"))));
    }

    @Test
    void unexpectedErrorIsSanitized500() throws Exception {
        when(service.getAllSuppliers()).thenThrow(new IllegalStateException("SENSITIVE_SQL_PASSWORD_SENTINEL"));
        error(mvc.perform(get(API)), 500, "INTERNAL_SERVER_ERROR")
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("SENSITIVE_SQL_PASSWORD_SENTINEL"))));
    }

    private ResultActions error(ResultActions result, int statusCode, String error) throws Exception {
        return result.andExpect(status().is(statusCode)).andExpect(jsonPath("$.status").value(statusCode))
                .andExpect(jsonPath("$.error").value(error)).andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private String path(String method) { return method.equals("POST") ? API : API + "/1"; }

    private MockHttpServletRequestBuilder request(String method, String path, String body) {
        return switch (method) {
            case "POST" -> post(path).contentType(MediaType.APPLICATION_JSON).content(body);
            case "PUT" -> put(path).contentType(MediaType.APPLICATION_JSON).content(body);
            case "DELETE" -> delete(path);
            default -> get(path);
        };
    }
}
