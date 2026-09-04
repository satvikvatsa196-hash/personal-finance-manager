package com.example.financemanager.service

import com.example.financemanager.dto.response.MonthlyReportResponse
import com.example.financemanager.dto.response.YearlyReportResponse
import com.example.financemanager.model.entity.User
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class ReportService(
    private val transactionRepository: TransactionRepository
) {

    @Transactional(readOnly = true)
    fun getMonthlyReport(user: User, year: Int, month: Int): MonthlyReportResponse {
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val categorySums = transactionRepository.getCategorySumsForUserBetweenDates(user, startDate, endDate)

        val totalIncome = categorySums.filter { it.type == CategoryType.INCOME }.associate { it.categoryName to it.total }
        val totalExpenses = categorySums.filter { it.type == CategoryType.EXPENSE }.associate { it.categoryName to it.total }
        
        val netIncome = totalIncome.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val netExpense = totalExpenses.values.fold(BigDecimal.ZERO, BigDecimal::add)

        return MonthlyReportResponse(
            month = month,
            year = year,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netIncome.subtract(netExpense)
        )
    }

    @Transactional(readOnly = true)
    fun getYearlyReport(user: User, year: Int): YearlyReportResponse {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        val categorySums = transactionRepository.getCategorySumsForUserBetweenDates(user, startDate, endDate)

        val totalIncome = categorySums.filter { it.type == CategoryType.INCOME }.associate { it.categoryName to it.total }
        val totalExpenses = categorySums.filter { it.type == CategoryType.EXPENSE }.associate { it.categoryName to it.total }
        
        val netIncome = totalIncome.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val netExpense = totalExpenses.values.fold(BigDecimal.ZERO, BigDecimal::add)

        return YearlyReportResponse(
            year = year,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netIncome.subtract(netExpense)
        )
    }
}
