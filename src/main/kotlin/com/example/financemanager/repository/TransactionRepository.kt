package com.example.financemanager.repository

import com.example.financemanager.model.entity.Transaction
import com.example.financemanager.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    fun findByUserOrderByDateDesc(user: User): List<Transaction>
    fun existsByCategoryId(categoryId: Long): Boolean

    @org.springframework.data.jpa.repository.Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.date >= :startDate AND t.category.type = :type")
    fun sumAmountByUserAndTypeSinceDate(
        @org.springframework.data.repository.query.Param("user") user: User,
        @org.springframework.data.repository.query.Param("startDate") startDate: java.time.LocalDate,
        @org.springframework.data.repository.query.Param("type") type: com.example.financemanager.model.enums.CategoryType
    ): java.math.BigDecimal?

    @org.springframework.data.jpa.repository.Query("""
        SELECT t.category.name as categoryName, t.category.type as type, SUM(t.amount) as total
        FROM Transaction t
        WHERE t.user = :user
        AND t.date >= :startDate AND t.date <= :endDate
        GROUP BY t.category.name, t.category.type
    """)
    fun getCategorySumsForUserBetweenDates(
        @org.springframework.data.repository.query.Param("user") user: User,
        @org.springframework.data.repository.query.Param("startDate") startDate: java.time.LocalDate,
        @org.springframework.data.repository.query.Param("endDate") endDate: java.time.LocalDate
    ): List<CategorySum>
}

interface CategorySum {
    val categoryName: String
    val type: com.example.financemanager.model.enums.CategoryType
    val total: java.math.BigDecimal
}
