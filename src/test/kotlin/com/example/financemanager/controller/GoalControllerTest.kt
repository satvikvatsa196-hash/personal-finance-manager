package com.example.financemanager.controller

import com.example.financemanager.dto.request.GoalRequest
import com.example.financemanager.dto.request.GoalUpdateRequest
import com.example.financemanager.model.entity.Category
import com.example.financemanager.model.entity.SavingsGoal
import com.example.financemanager.model.entity.Transaction
import com.example.financemanager.model.entity.User
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.CategoryRepository
import com.example.financemanager.repository.SavingsGoalRepository
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
class GoalControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var goalRepository: SavingsGoalRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        transactionRepository.deleteAll()
        goalRepository.deleteAll()
        categoryRepository.findAll().forEach { if (it.user != null) categoryRepository.delete(it) }
        userRepository.deleteAll()

        userRepository.save(User("user1@example.com", "hash", "User One", "123"))
        userRepository.save(User("user2@example.com", "hash", "User Two", "456"))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should create goal and calculate zero progress initially`() {
        val request = GoalRequest("Emergency Fund", BigDecimal("5000.00"), LocalDate.now().plusYears(1), null)

        mockMvc.perform(
            post("/api/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.currentProgress").value(0.0))
            .andExpect(jsonPath("$.progressPercentage").value(0.0))
            .andExpect(jsonPath("$.remainingAmount").value(5000.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should calculate progress correctly with mixed transactions`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salaryCat = categoryRepository.findByUserIsNullAndName("Salary")!!
        val foodCat = categoryRepository.findByUserIsNullAndName("Food")!!
        
        // Goal starts on Jan 1
        val goal = goalRepository.save(SavingsGoal("Vacation", BigDecimal("1000"), LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), user1))

        // Transaction before start date (Should NOT count)
        transactionRepository.save(Transaction(BigDecimal("5000"), LocalDate.of(2025, 12, 31), salaryCat, "Old", user1))
        
        // Transactions after start date (Should count)
        transactionRepository.save(Transaction(BigDecimal("2000"), LocalDate.of(2026, 2, 1), salaryCat, "Income 1", user1))
        transactionRepository.save(Transaction(BigDecimal("500"), LocalDate.of(2026, 2, 15), foodCat, "Expense 1", user1))
        transactionRepository.save(Transaction(BigDecimal("1000"), LocalDate.of(2026, 3, 1), salaryCat, "Income 2", user1))
        transactionRepository.save(Transaction(BigDecimal("1000"), LocalDate.of(2026, 3, 15), foodCat, "Expense 2", user1))

        // Total Income: 3000
        // Total Expense: 1500
        // Net Savings = 1500
        // Target = 1000
        // Progress = 1500 / 1000 = 150% -> Capped at 100%
        // Remaining = 0

        mockMvc.perform(get("/api/goals/${goal.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentProgress").value(1500.0))
            .andExpect(jsonPath("$.progressPercentage").value(100.0))
            .andExpect(jsonPath("$.remainingAmount").value(0.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should calculate progress percentage accurately under 100`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salaryCat = categoryRepository.findByUserIsNullAndName("Salary")!!
        val goal = goalRepository.save(SavingsGoal("Car", BigDecimal("10000"), LocalDate.now().minusDays(10), LocalDate.now().plusYears(1), user1))
        
        transactionRepository.save(Transaction(BigDecimal("2500"), LocalDate.now(), salaryCat, "Income", user1))

        mockMvc.perform(get("/api/goals/${goal.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentProgress").value(2500.0))
            .andExpect(jsonPath("$.progressPercentage").value(25.0))
            .andExpect(jsonPath("$.remainingAmount").value(7500.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `deleted transactions do not count`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salaryCat = categoryRepository.findByUserIsNullAndName("Salary")!!
        val goal = goalRepository.save(SavingsGoal("Test", BigDecimal("1000"), LocalDate.now().minusDays(10), LocalDate.now().plusYears(1), user1))
        
        val txn = transactionRepository.save(Transaction(BigDecimal("500"), LocalDate.now(), salaryCat, "Income", user1))
        
        // Check progress is 500
        mockMvc.perform(get("/api/goals/${goal.id}")).andExpect(jsonPath("$.currentProgress").value(500.0))
        
        // Delete transaction
        transactionRepository.delete(txn)
        
        // Check progress is 0
        mockMvc.perform(get("/api/goals/${goal.id}")).andExpect(jsonPath("$.currentProgress").value(0.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should enforce cross-user isolation`() {
        val user2 = userRepository.findByUsername("user2@example.com")!!
        val goal = goalRepository.save(SavingsGoal("Secret", BigDecimal("1000"), LocalDate.now(), LocalDate.now().plusYears(1), user2))

        mockMvc.perform(get("/api/goals/${goal.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to create goal with past target date`() {
        val request = GoalRequest("Past", BigDecimal("5000.00"), LocalDate.now().minusDays(1), null)

        mockMvc.perform(
            post("/api/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to create goal with negative target amount`() {
        val request = GoalRequest("Negative", BigDecimal("-100"), LocalDate.now().plusDays(1), null)

        mockMvc.perform(
            post("/api/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should update goal successfully`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val goal = goalRepository.save(SavingsGoal("Test", BigDecimal("1000"), LocalDate.now(), LocalDate.now().plusYears(1), user1))

        val request = GoalUpdateRequest(BigDecimal("2000"), LocalDate.now().plusYears(2))

        mockMvc.perform(
            put("/api/goals/${goal.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.targetAmount").value(2000.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should delete goal successfully`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val goal = goalRepository.save(SavingsGoal("Test", BigDecimal("1000"), LocalDate.now(), LocalDate.now().plusYears(1), user1))

        mockMvc.perform(delete("/api/goals/${goal.id}"))
            .andExpect(status().isOk)

        assertThat(goalRepository.findById(goal.id!!)).isEmpty
    }
}
