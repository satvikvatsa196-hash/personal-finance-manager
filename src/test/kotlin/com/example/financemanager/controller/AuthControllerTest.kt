package com.example.financemanager.controller

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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
    }

    @Test
    fun `should register successfully`() {
        val request = RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("User registered successfully"))
            .andExpect(jsonPath("$.userId").isNumber)

        val user = userRepository.findByUsername("user@example.com")
        assertThat(user).isNotNull
        assertThat(user?.fullName).isEqualTo("John Doe")
        // Password is hashed and never returned
        assertThat(user?.passwordHash).isNotEqualTo("password123")
        assertThat(passwordEncoder.matches("password123", user?.passwordHash)).isTrue
    }

    @Test
    fun `should return 409 for duplicate email`() {
        userRepository.save(User("user@example.com", "hash", "John", "123"))

        val request = RegisterRequest("user@example.com", "password123", "Jane Doe", "+0987654321")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Username already exists"))
    }

    @Test
    fun `should return 400 for invalid email`() {
        val request = RegisterRequest("not-an-email", "password123", "John Doe", "+1234567890")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for invalid password (too short)`() {
        val request = RegisterRequest("user@example.com", "short", "John Doe", "+1234567890")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for missing fields`() {
        val request = "{}"

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should login successfully and establish session`() {
        userRepository.save(User("user@example.com", passwordEncoder.encode("password123"), "John", "123"))

        val request = LoginRequest("user@example.com", "password123")

        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andExpect(cookie().exists("JSESSIONID"))
            .andReturn()

        val cookie = result.response.getCookie("JSESSIONID")!!

        // Test authenticated endpoint with cookie
        mockMvc.perform(
            get("/api/test/me")
                .cookie(cookie)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("user@example.com"))
    }

    @Test
    fun `should return 401 for invalid credentials`() {
        userRepository.save(User("user@example.com", passwordEncoder.encode("password123"), "John", "123"))

        val request = LoginRequest("user@example.com", "wrongpassword")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 for unauthenticated request to protected endpoint`() {
        // Any unmapped endpoint or protected endpoint should return 401
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should logout successfully and invalidate session`() {
        userRepository.save(User("user@example.com", passwordEncoder.encode("password123"), "John", "123"))
        val request = LoginRequest("user@example.com", "password123")

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val cookie = loginResult.response.getCookie("JSESSIONID")

        mockMvc.perform(
            post("/api/auth/logout")
                .cookie(cookie)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logout successful"))
            .andExpect(cookie().maxAge("JSESSIONID", 0)) // Cookie deleted
    }
}
