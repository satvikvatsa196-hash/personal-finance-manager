package com.example.financemanager.controller

import com.example.financemanager.dto.request.TransactionRequest
import com.example.financemanager.dto.request.TransactionUpdateRequest
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
class TransactionControllerTest {

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
        categoryRepository.findAll().forEach { if (it.user != null) categoryRepository.delete(it) }
        userRepository.deleteAll()

        userRepository.save(User("user1@example.com", "hash", "User One", "123"))
        userRepository.save(User("user2@example.com", "hash", "User Two", "456"))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should create transaction successfully`() {
        val request = TransactionRequest(BigDecimal("50000.00"), LocalDate.now(), "Salary", "January Salary")

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(50000.0))
            .andExpect(jsonPath("$.category").value("Salary"))
            .andExpect(jsonPath("$.type").value("INCOME"))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to create transaction with negative amount`() {
        val request = TransactionRequest(BigDecimal("-10.00"), LocalDate.now(), "Salary", "Desc")

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to create transaction with future date`() {
        val request = TransactionRequest(BigDecimal("10.00"), LocalDate.now().plusDays(1), "Salary", "Desc")

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should fail to create transaction with inaccessible category`() {
        val user2 = userRepository.findByUsername("user2@example.com")!!
        categoryRepository.save(Category("Secret", CategoryType.EXPENSE, true, user2))

        val request = TransactionRequest(BigDecimal("10.00"), LocalDate.now(), "Secret", "Desc")

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should list and filter transactions`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val cat = categoryRepository.findByUserIsNullAndName("Salary")!!
        
        transactionRepository.save(Transaction(BigDecimal("100"), LocalDate.now().minusDays(2), cat, "1", user1))
        transactionRepository.save(Transaction(BigDecimal("200"), LocalDate.now(), cat, "2", user1))

        // Listing, newest first
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions[0].amount").value(200.0))
            .andExpect(jsonPath("$.transactions[1].amount").value(100.0))

        // Date filtering
        val startDate = LocalDate.now().minusDays(1).toString()
        mockMvc.perform(get("/api/transactions?startDate=$startDate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions.length()").value(1))
            .andExpect(jsonPath("$.transactions[0].amount").value(200.0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should enforce cross-user isolation on read`() {
        val user2 = userRepository.findByUsername("user2@example.com")!!
        val cat = categoryRepository.findByUserIsNullAndName("Food")!!
        transactionRepository.save(Transaction(BigDecimal("100"), LocalDate.now(), cat, "User2 txn", user2))

        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions.length()").value(0))
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should update transaction successfully`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val salaryCat = categoryRepository.findByUserIsNullAndName("Salary")!!
        val foodCat = categoryRepository.findByUserIsNullAndName("Food")!!
        
        val txn = transactionRepository.save(Transaction(BigDecimal("100"), LocalDate.now().minusDays(5), salaryCat, "Desc", user1))

        val request = TransactionUpdateRequest(BigDecimal("200"), "Food", "Updated Desc")

        mockMvc.perform(
            put("/api/transactions/${txn.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(200.0))
            .andExpect(jsonPath("$.category").value("Food"))

        val updated = transactionRepository.findById(txn.id!!).get()
        assertThat(updated.date).isEqualTo(txn.date) // Verify date is unchanged
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should delete transaction successfully`() {
        val user1 = userRepository.findByUsername("user1@example.com")!!
        val cat = categoryRepository.findByUserIsNullAndName("Salary")!!
        val txn = transactionRepository.save(Transaction(BigDecimal("100"), LocalDate.now(), cat, "Desc", user1))

        mockMvc.perform(delete("/api/transactions/${txn.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Transaction deleted successfully"))

        assertThat(transactionRepository.findById(txn.id!!)).isEmpty
    }

    @Test
    @WithUserDetails("user1@example.com")
    fun `should return 404 when updating another user's transaction`() {
        val user2 = userRepository.findByUsername("user2@example.com")!!
        val cat = categoryRepository.findByUserIsNullAndName("Salary")!!
        val txn = transactionRepository.save(Transaction(BigDecimal("100"), LocalDate.now(), cat, "Desc", user2))

        val request = TransactionUpdateRequest(BigDecimal("200"), "Food", "Updated Desc")

        mockMvc.perform(
            put("/api/transactions/${txn.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }
}
