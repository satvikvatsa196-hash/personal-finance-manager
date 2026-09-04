package com.example.financemanager.controller

import com.example.financemanager.dto.request.TransactionRequest
import com.example.financemanager.dto.request.TransactionUpdateRequest
import com.example.financemanager.dto.response.MessageResponse
import com.example.financemanager.dto.response.TransactionDto
import com.example.financemanager.dto.response.TransactionListResponse
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.security.UserPrincipal
import com.example.financemanager.service.TransactionService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionService: TransactionService
) {

    @PostMapping
    fun createTransaction(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: TransactionRequest
    ): ResponseEntity<TransactionDto> {
        val response = transactionService.createTransaction(userPrincipal.getUser(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getTransactions(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) type: CategoryType?
    ): ResponseEntity<TransactionListResponse> {
        val response = transactionService.getTransactions(userPrincipal.getUser(), startDate, endDate, categoryId, category, type)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    fun updateTransaction(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: TransactionUpdateRequest
    ): ResponseEntity<TransactionDto> {
        val response = transactionService.updateTransaction(userPrincipal.getUser(), id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun deleteTransaction(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<MessageResponse> {
        transactionService.deleteTransaction(userPrincipal.getUser(), id)
        return ResponseEntity.ok(MessageResponse("Transaction deleted successfully"))
    }
}
