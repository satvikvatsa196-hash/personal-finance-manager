package com.example.financemanager.integration

import com.example.financemanager.dto.request.LoginRequest
import com.example.financemanager.dto.request.RegisterRequest
import com.example.financemanager.model.entity.User
import com.example.financemanager.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFixTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
    }

    @Test
    fun `Swagger OpenAPI documentation is publicly accessible`() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
    }

    @Test
    fun `Login response JSESSIONID cookie must include SameSite Strict and Secure attributes`() {
        val registerRequest = RegisterRequest("cookie@example.com", "password123", "Cookie User", "123")
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        ).andExpect(status().isCreated)

        val loginRequest = LoginRequest("cookie@example.com", "password123")
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isOk).andReturn()

        val setCookieHeader = result.response.getHeader("Set-Cookie")
        
        assertThat(setCookieHeader).isNotNull()
        assertThat(setCookieHeader).contains("JSESSIONID")
        
        // Assert security attributes are present in the raw Set-Cookie header
        assertThat(setCookieHeader).containsIgnoringCase("SameSite=Strict")
        assertThat(setCookieHeader).containsIgnoringCase("Secure")
        assertThat(setCookieHeader).containsIgnoringCase("HttpOnly")
    }
}
