package com.example.financemanager.integration

import com.example.financemanager.dto.request.*
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.CategoryRepository
import com.example.financemanager.repository.SavingsGoalRepository
import com.example.financemanager.repository.TransactionRepository
import com.example.financemanager.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

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

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @BeforeEach
    fun setup() {
        transactionRepository.deleteAll()
        goalRepository.deleteAll()
        categoryRepository.findAll().forEach { if (it.user != null) categoryRepository.delete(it) }
        userRepository.deleteAll()
    }

    private fun register(username: String): String {
        val request = RegisterRequest(username, "password123", "Integration User", "+1234567890")
        val content = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        
        // Return userId as String for reference if needed
        return objectMapper.readTree(content).get("userId").asText()
    }

    private fun login(username: String): Cookie {
        val request = LoginRequest(username, "password123")
        return mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk).andReturn().response.getCookie("JSESSIONID")!!
    }

    @Test
    fun `FLOW 1 - Register, Login, Access protected endpoint, Logout, Verify rejected`() {
        register("flow1@example.com")
        val sessionCookie = login("flow1@example.com")

        // Access protected endpoint
        mockMvc.perform(get("/api/categories").cookie(sessionCookie))
            .andExpect(status().isOk)

        // Logout
        mockMvc.perform(post("/api/auth/logout").cookie(sessionCookie))
            .andExpect(status().isOk)

        // Verify rejected
        mockMvc.perform(get("/api/categories").cookie(sessionCookie))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `FLOW 2 - Register, Login, Create custom category, Create transaction, Retrieve transaction`() {
        register("flow2@example.com")
        val sessionCookie = login("flow2@example.com")

        // Create custom category
        val catRequest = CategoryRequest("Side Hustle", CategoryType.INCOME)
        mockMvc.perform(
            post("/api/categories")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catRequest))
        ).andExpect(status().isCreated)

        // Create transaction
        val txnRequest = TransactionRequest(BigDecimal("150.00"), LocalDate.now(), "Side Hustle", "First gig")
        val txnContent = mockMvc.perform(
            post("/api/transactions")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txnRequest))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val txnId = objectMapper.readTree(txnContent).get("id").asLong()

        // Retrieve transaction
        mockMvc.perform(get("/api/transactions").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions[0].id").value(txnId))
            .andExpect(jsonPath("$.transactions[0].category").value("Side Hustle"))
    }

    @Test
    fun `FLOW 3 - Create, Update, Verify date unchanged, Delete transaction`() {
        register("flow3@example.com")
        val sessionCookie = login("flow3@example.com")

        val txnRequest = TransactionRequest(BigDecimal("150.00"), LocalDate.now().minusDays(5), "Salary", "Original")
        val txnContent = mockMvc.perform(
            post("/api/transactions")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txnRequest))
        ).andReturn().response.contentAsString

        val txnId = objectMapper.readTree(txnContent).get("id").asLong()

        // Update transaction (change amount, category, description)
        val updateRequest = TransactionUpdateRequest(BigDecimal("250.00"), "Food", "Updated")
        mockMvc.perform(
            put("/api/transactions/$txnId")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpect(status().isOk)

        // Retrieve and verify date is unchanged
        mockMvc.perform(get("/api/transactions").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions[0].amount").value(250.0))
            .andExpect(jsonPath("$.transactions[0].category").value("Food"))
            .andExpect(jsonPath("$.transactions[0].date").value(LocalDate.now().minusDays(5).toString()))

        // Delete transaction
        mockMvc.perform(delete("/api/transactions/$txnId").cookie(sessionCookie))
            .andExpect(status().isOk)

        // Verify deleted
        mockMvc.perform(get("/api/transactions").cookie(sessionCookie))
            .andExpect(jsonPath("$.transactions").isEmpty)
    }

    @Test
    fun `FLOW 4 - Create savings goal, Create income expense transactions, Verify calculated progress`() {
        register("flow4@example.com")
        val sessionCookie = login("flow4@example.com")

        // Create Goal
        val goalRequest = GoalRequest("Test Goal", BigDecimal("1000.00"), LocalDate.now().plusMonths(6), LocalDate.now())
        val goalContent = mockMvc.perform(
            post("/api/goals")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest))
        ).andReturn().response.contentAsString

        val goalId = objectMapper.readTree(goalContent).get("id").asLong()

        // Transactions
        val txn1 = TransactionRequest(BigDecimal("500.00"), LocalDate.now(), "Salary", "Income") // Income
        val txn2 = TransactionRequest(BigDecimal("100.00"), LocalDate.now(), "Food", "Expense") // Expense

        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn1)))
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn2)))

        // Verify progress (500 - 100 = 400)
        mockMvc.perform(get("/api/goals/$goalId").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentProgress").value(400.0))
            .andExpect(jsonPath("$.progressPercentage").value(40.0))
            .andExpect(jsonPath("$.remainingAmount").value(600.0))
    }

    @Test
    fun `FLOW 5 - Create transactions, Generate monthly report, Verify totals`() {
        register("flow5@example.com")
        val sessionCookie = login("flow5@example.com")

        val currentMonth = LocalDate.now()
        val txn1 = TransactionRequest(BigDecimal("1000.00"), currentMonth, "Salary", "Income")
        val txn2 = TransactionRequest(BigDecimal("200.00"), currentMonth, "Food", "Expense")

        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn1)))
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn2)))

        mockMvc.perform(get("/api/reports/monthly/${currentMonth.year}/${currentMonth.monthValue}").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome.Salary").value(1000.0))
            .andExpect(jsonPath("$.totalExpenses.Food").value(200.0))
            .andExpect(jsonPath("$.netSavings").value(800.0))
    }

    @Test
    fun `FLOW 6 - Create transactions, Generate yearly report, Verify totals`() {
        register("flow6@example.com")
        val sessionCookie = login("flow6@example.com")

        val txn1 = TransactionRequest(BigDecimal("5000.00"), LocalDate.of(2024, 1, 15), "Salary", "Income")
        val txn2 = TransactionRequest(BigDecimal("2000.00"), LocalDate.of(2024, 7, 10), "Salary", "Income")
        val txn3 = TransactionRequest(BigDecimal("500.00"), LocalDate.of(2024, 12, 1), "Rent", "Expense")

        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn1)))
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn2)))
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn3)))

        mockMvc.perform(get("/api/reports/yearly/2024").cookie(sessionCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIncome.Salary").value(7000.0))
            .andExpect(jsonPath("$.totalExpenses.Rent").value(500.0))
            .andExpect(jsonPath("$.netSavings").value(6500.0))
    }

    @Test
    fun `MULTI-USER SECURITY - Verify cross user isolation`() {
        register("userA@example.com")
        val cookieA = login("userA@example.com")

        register("userB@example.com")
        val cookieB = login("userB@example.com")

        // User A creates category
        val catRequest = CategoryRequest("A's Category", CategoryType.INCOME)
        mockMvc.perform(post("/api/categories").cookie(cookieA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(catRequest)))

        // User B tries to use A's category for a transaction
        val txnRequest = TransactionRequest(BigDecimal("100"), LocalDate.now(), "A's Category", "")
        mockMvc.perform(post("/api/transactions").cookie(cookieB).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txnRequest)))
            .andExpect(status().isBadRequest) // Inaccessible category

        // User A creates a transaction
        val txnContent = mockMvc.perform(post("/api/transactions").cookie(cookieA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(TransactionRequest(BigDecimal("100"), LocalDate.now(), "Salary", ""))))
            .andReturn().response.contentAsString
        val txnId = objectMapper.readTree(txnContent).get("id").asLong()

        // User B tries to access A's transaction
        mockMvc.perform(delete("/api/transactions/$txnId").cookie(cookieB))
            .andExpect(status().isNotFound)
            
        // User A creates a goal
        val goalContent = mockMvc.perform(post("/api/goals").cookie(cookieA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(GoalRequest("A's Goal", BigDecimal("1000"), LocalDate.now().plusMonths(1), null))))
            .andReturn().response.contentAsString
        val goalId = objectMapper.readTree(goalContent).get("id").asLong()
        
        // User B tries to access A's goal
        mockMvc.perform(get("/api/goals/$goalId").cookie(cookieB))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `VALIDATION - Missing fields, invalid values, invalid dates`() {
        register("validation@example.com")
        val sessionCookie = login("validation@example.com")

        // Missing fields in transaction
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)

        // Invalid amount (negative)
        val negativeTxn = TransactionRequest(BigDecimal("-10.00"), LocalDate.now(), "Salary", "")
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(negativeTxn)))
            .andExpect(status().isBadRequest)

        // Invalid date (future transaction)
        val futureTxn = TransactionRequest(BigDecimal("10.00"), LocalDate.now().plusDays(1), "Salary", "")
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(futureTxn)))
            .andExpect(status().isBadRequest)
            
        // Invalid goal date (past target date)
        val invalidGoal = GoalRequest("Test", BigDecimal("100"), LocalDate.now().minusDays(1), null)
        mockMvc.perform(post("/api/goals").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidGoal)))
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `CONFLICTS - Duplicate user, duplicate category, referenced category deletion`() {
        register("conflict@example.com")
        val sessionCookie = login("conflict@example.com")
        
        // Duplicate User
        val dupUser = RegisterRequest("conflict@example.com", "password123", "Dup", "+123")
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dupUser)))
            .andExpect(status().isConflict)
            
        // Create custom category
        val catRequest = CategoryRequest("Custom", CategoryType.INCOME)
        mockMvc.perform(post("/api/categories").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(catRequest)))
            .andExpect(status().isCreated)
            
        // Duplicate category
        mockMvc.perform(post("/api/categories").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(catRequest)))
            .andExpect(status().isConflict)
            
        // Create transaction referencing it
        val txn = TransactionRequest(BigDecimal("10"), LocalDate.now(), "Custom", "")
        mockMvc.perform(post("/api/transactions").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txn)))
            
        // Delete referenced category
        mockMvc.perform(delete("/api/categories/Custom").cookie(sessionCookie))
            .andExpect(status().isBadRequest)
    }
}
