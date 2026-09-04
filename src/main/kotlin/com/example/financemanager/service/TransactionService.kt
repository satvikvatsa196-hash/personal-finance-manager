package com.example.financemanager.service

import com.example.financemanager.dto.request.TransactionRequest
import com.example.financemanager.dto.request.TransactionUpdateRequest
import com.example.financemanager.dto.response.TransactionDto
import com.example.financemanager.dto.response.TransactionListResponse
import com.example.financemanager.exception.BadRequestException
import com.example.financemanager.exception.ForbiddenException
import com.example.financemanager.exception.ResourceNotFoundException
import com.example.financemanager.model.entity.Transaction
import com.example.financemanager.model.entity.User
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.CategoryRepository
import com.example.financemanager.repository.TransactionRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun createTransaction(user: User, request: TransactionRequest): TransactionDto {
        val category = categoryRepository.findAccessibleCategoryByName(user, request.category!!)
            ?: throw BadRequestException("Category not found or not accessible")

        val transaction = Transaction(
            amount = request.amount!!,
            date = request.date!!,
            category = category,
            description = request.description,
            user = user
        )

        val saved = transactionRepository.save(transaction)
        return mapToDto(saved)
    }

    @Transactional(readOnly = true)
    fun getTransactions(
        user: User,
        startDate: LocalDate?,
        endDate: LocalDate?,
        categoryId: Long?,
        type: CategoryType?
    ): TransactionListResponse {
        val spec = Specification<Transaction> { root, query, cb ->
            val predicates = mutableListOf(cb.equal(root.get<User>("user"), user))

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate))
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate))
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get<Any>("category").get<Long>("id"), categoryId))
            }
            if (type != null) {
                predicates.add(cb.equal(root.get<Any>("category").get<CategoryType>("type"), type))
            }

            query.orderBy(cb.desc(root.get<LocalDate>("date")), cb.desc(root.get<Long>("id")))
            cb.and(*predicates.toTypedArray())
        }

        val transactions = transactionRepository.findAll(spec)
        return TransactionListResponse(transactions.map { mapToDto(it) })
    }

    @Transactional
    fun updateTransaction(user: User, id: Long, request: TransactionUpdateRequest): TransactionDto {
        val transaction = getTransactionForUser(user, id)

        val category = categoryRepository.findAccessibleCategoryByName(user, request.category!!)
            ?: throw BadRequestException("Category not found or not accessible")

        transaction.amount = request.amount!!
        transaction.category = category
        transaction.description = request.description
        // Date cannot be changed

        val updated = transactionRepository.save(transaction)
        return mapToDto(updated)
    }

    @Transactional
    fun deleteTransaction(user: User, id: Long) {
        val transaction = getTransactionForUser(user, id)
        transactionRepository.delete(transaction)
    }

    private fun getTransactionForUser(user: User, id: Long): Transaction {
        val transaction = transactionRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Transaction not found") }

        if (transaction.user.id != user.id) {
            throw ResourceNotFoundException("Transaction not found") // Or Forbidden, but 404 is standard to not leak existence
        }

        return transaction
    }

    private fun mapToDto(transaction: Transaction): TransactionDto {
        return TransactionDto(
            id = transaction.id!!,
            amount = transaction.amount,
            date = transaction.date,
            category = transaction.category.name,
            description = transaction.description,
            type = transaction.category.type
        )
    }
}
