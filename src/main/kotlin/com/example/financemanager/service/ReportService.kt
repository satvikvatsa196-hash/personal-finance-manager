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

        val totalIncome = mutableMapOf<String, BigDecimal>()
        val totalExpenses = mutableMapOf<String, BigDecimal>()
        var netIncome = BigDecimal.ZERO
        var netExpense = BigDecimal.ZERO

        for (sum in categorySums) {
            if (sum.type == CategoryType.INCOME) {
                totalIncome[sum.categoryName] = sum.total
                netIncome = netIncome.add(sum.total)
            } else {
                totalExpenses[sum.categoryName] = sum.total
                netExpense = netExpense.add(sum.total)
            }
        }

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

        val totalIncome = mutableMapOf<String, BigDecimal>()
        val totalExpenses = mutableMapOf<String, BigDecimal>()
        var netIncome = BigDecimal.ZERO
        var netExpense = BigDecimal.ZERO

        for (sum in categorySums) {
            if (sum.type == CategoryType.INCOME) {
                totalIncome[sum.categoryName] = sum.total
                netIncome = netIncome.add(sum.total)
            } else {
                totalExpenses[sum.categoryName] = sum.total
                netExpense = netExpense.add(sum.total)
            }
        }

        return YearlyReportResponse(
            year = year,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netIncome.subtract(netExpense)
        )
    }
}
