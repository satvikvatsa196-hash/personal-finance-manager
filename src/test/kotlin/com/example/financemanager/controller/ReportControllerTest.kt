package com.example.financemanager.controller

import com.example.financemanager.model.entity.Transaction
import com.example.financemanager.model.entity.User
import com.example.financemanager.repository.CategoryRepository
import com.example.financemanager.repository.TransactionRepository
import com.example.financemanager.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @BeforeEach
    fun setup() {
        transactionRepository.deleteAll()
        categoryRepository.findAll().forEach { if (it.user != null) categoryRepository.delete(it) }
        userRepository.deleteAll()

        userRepository.save(User("user1@example.com", "hash", "User One", "123"))
        userRepository.save(User("user2@example.com", "hash", "User Two", "456"))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should return empty report when no transactions`() {
        mockMvc.perform(get("/api/reports/monthly/2024/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(1))
            .andExpect(jsonPath("$.year").value(2024))
            .andExpect(jsonPath("$.netSavings").value(0.0))
            .andExpect(jsonPath("$.totalIncome").isEmpty)
            .andExpect(jsonPath("$.totalExpenses").isEmpty)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should calculate mixed transactions for monthly report accurately`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salary = categoryRepository.findByUserIsNullAndName("Salary")!!
        val food = categoryRepository.findByUserIsNullAndName("Food")!!
        val rent = categoryRepository.findByUserIsNullAndName("Rent")!!

        // Jan 2024 Transactions
        transactionRepository.save(Transaction(BigDecimal("5000"), LocalDate.of(2024, 1, 15), salary, "Income", user1))
        transactionRepository.save(Transaction(BigDecimal("500"), LocalDate.of(2024, 1, 20), food, "Food", user1))
        transactionRepository.save(Transaction(BigDecimal("1500"), LocalDate.of(2024, 1, 1), rent, "Rent", user1))

        // Feb 2024 (should not be included)
        transactionRepository.save(Transaction(BigDecimal("100"), LocalDate.of(2024, 2, 1), food, "Food next month", user1))

        mockMvc.perform(get("/api/reports/monthly/2024/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome.Salary").value(5000.0))
            .andExpect(jsonPath("$.totalExpenses.Food").value(500.0))
            .andExpect(jsonPath("$.totalExpenses.Rent").value(1500.0))
            .andExpect(jsonPath("$.netSavings").value(3000.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should calculate mixed transactions for yearly report accurately`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salary = categoryRepository.findByUserIsNullAndName("Salary")!!
        val food = categoryRepository.findByUserIsNullAndName("Food")!!

        // 2024 Transactions across months
        transactionRepository.save(Transaction(BigDecimal("5000"), LocalDate.of(2024, 1, 15), salary, "Income 1", user1))
        transactionRepository.save(Transaction(BigDecimal("5000"), LocalDate.of(2024, 6, 15), salary, "Income 6", user1))
        transactionRepository.save(Transaction(BigDecimal("1000"), LocalDate.of(2024, 12, 31), food, "Food", user1))

        // 2023 (should not be included)
        transactionRepository.save(Transaction(BigDecimal("10000"), LocalDate.of(2023, 12, 31), salary, "Last year", user1))

        mockMvc.perform(get("/api/reports/yearly/2024"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome.Salary").value(10000.0))
            .andExpect(jsonPath("$.totalExpenses.Food").value(1000.0))
            .andExpect(jsonPath("$.netSavings").value(9000.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should enforce cross-user isolation`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val user2 = userRepository.findByUsername("user2@example.com")!!
        val salary = categoryRepository.findByUserIsNullAndName("Salary")!!

        transactionRepository.save(Transaction(BigDecimal("1000"), LocalDate.of(2024, 1, 1), salary, "User 1", user1))
        transactionRepository.save(Transaction(BigDecimal("5000"), LocalDate.of(2024, 1, 1), salary, "User 2", user2))

        // user1 should only see their 1000
        mockMvc.perform(get("/api/reports/monthly/2024/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome.Salary").value(1000.0))
            .andExpect(jsonPath("$.netSavings").value(1000.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `deleted transactions do not count`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salary = categoryRepository.findByUserIsNullAndName("Salary")!!
        
        val txn = transactionRepository.save(Transaction(BigDecimal("1000"), LocalDate.of(2024, 1, 1), salary, "Income", user1))
        transactionRepository.delete(txn)

        mockMvc.perform(get("/api/reports/monthly/2024/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.netSavings").value(0.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should return 400 for invalid month or year`() {
        mockMvc.perform(get("/api/reports/monthly/2024/13"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/reports/yearly/1000"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 401 if unauthenticated`() {
        mockMvc.perform(get("/api/reports/monthly/2024/1"))
            .andExpect(status().isUnauthorized)
    }
}
