package com.example.financemanager.controller

import com.example.financemanager.dto.request.CategoryRequest
import com.example.financemanager.model.entity.Category
import com.example.financemanager.model.entity.Transaction
import com.example.financemanager.model.entity.User
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.CategoryRepository
import com.example.financemanager.repository.TransactionRepository
import com.example.financemanager.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        transactionRepository.deleteAll()
        
        // Delete all categories except defaults (which are created by DataInitializer)
        categoryRepository.findAll().forEach {
            if (it.user != null) {
                categoryRepository.delete(it)
            }
        }
        
        userRepository.deleteAll()

        // Create test users
        userRepository.save(User("user1@example.com", "hash", "User One", "123"))
        userRepository.save(User("user2@example.com", "hash", "User Two", "456"))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should list default and user custom categories`() {
        val user = userRepository.findByUsername("user1@example.com")!!
        categoryRepository.save(Category("Side Hustle", CategoryType.INCOME, true, user))

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories").isArray)
            .andExpect(jsonPath("$.categories[?(@.name == 'Salary')]").exists()) // Default
            .andExpect(jsonPath("$.categories[?(@.name == 'Side Hustle')]").exists()) // Custom
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should create custom category successfully`() {
        val request = CategoryRequest("Freelance", CategoryType.INCOME)

        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Freelance"))
            .andExpect(jsonPath("$.type").value("INCOME"))
            .andExpect(jsonPath("$.isCustom").value(true))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to create duplicate custom category`() {
        val user = userRepository.findByUsername("user1@example.com")!!
        categoryRepository.save(Category("Freelance", CategoryType.INCOME, true, user))

        val request = CategoryRequest("Freelance", CategoryType.EXPENSE)

        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should delete custom category successfully`() {
        val user = userRepository.findByUsername("user1@example.com")!!
        categoryRepository.save(Category("Freelance", CategoryType.INCOME, true, user))

        mockMvc.perform(delete("/api/categories/Freelance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Category deleted successfully"))

        assertThat(categoryRepository.findByUserAndName(user, "Freelance")).isNull()
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to delete default category`() {
        mockMvc.perform(delete("/api/categories/Salary"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to delete category owned by another user`() {
        val user2 = userRepository.findByUsername("user2@example.com")!!
        categoryRepository.save(Category("Secret", CategoryType.EXPENSE, true, user2))

        mockMvc.perform(delete("/api/categories/Secret"))
            .andExpect(status().isNotFound) // Because user1 can't find user2's category
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to delete referenced custom category`() {
        val user = userRepository.findByUsername("user1@example.com")!!
        val category = categoryRepository.save(Category("Freelance", CategoryType.INCOME, true, user))

        transactionRepository.save(
            Transaction(
                amount = BigDecimal("100.00"),
                date = LocalDate.now(),
                category = category,
                description = "Test",
                user = user
            )
        )

        mockMvc.perform(delete("/api/categories/Freelance"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 401 if unauthenticated`() {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isUnauthorized)
    }
}
