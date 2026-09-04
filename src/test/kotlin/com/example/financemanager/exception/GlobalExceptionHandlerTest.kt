package com.example.financemanager.exception

import com.example.financemanager.controller.TestController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(TestController::class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test
class GlobalExceptionHandlerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 400 with details on validation failure`() {
        val requestBody = """{"name": "", "amount": -10}"""

        mockMvc.perform(
            post("/api/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details").isArray)
    }

    @Test
    fun `should return 400 on malformed JSON`() {
        mockMvc.perform(
            post("/api/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Malformed JSON request"))
    }

    @Test
    fun `should return 400 on BadRequestException`() {
        mockMvc.perform(get("/api/test/bad-request"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Bad request triggered"))
    }

    @Test
    fun `should return 401 on UnauthorizedException`() {
        mockMvc.perform(get("/api/test/unauthorized"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized access"))
    }

    @Test
    fun `should return 403 on ForbiddenException`() {
        mockMvc.perform(get("/api/test/forbidden"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access forbidden"))
    }

    @Test
    fun `should return 404 on ResourceNotFoundException`() {
        mockMvc.perform(get("/api/test/not-found"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Resource not found"))
    }

    @Test
    fun `should return 409 on ConflictException`() {
        mockMvc.perform(get("/api/test/conflict"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Conflict occurred"))
    }

    @Test
    fun `should return 500 without exposing stack trace on general Exception`() {
        mockMvc.perform(get("/api/test/internal"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.details").doesNotExist())
    }
}
