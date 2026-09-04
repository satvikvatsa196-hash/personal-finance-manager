package com.example.financemanager.controller

import com.example.financemanager.dto.response.MonthlyReportResponse
import com.example.financemanager.dto.response.YearlyReportResponse
import com.example.financemanager.exception.BadRequestException
import com.example.financemanager.security.UserPrincipal
import com.example.financemanager.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService
) {

    @GetMapping("/monthly/{year}/{month}")
    fun getMonthlyReport(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable year: Int,
        @PathVariable month: Int
    ): ResponseEntity<MonthlyReportResponse> {
        if (month !in 1..12) {
            throw BadRequestException("Month must be between 1 and 12")
        }
        if (year < 1900 || year > 2100) {
            throw BadRequestException("Invalid year")
        }
        val response = reportService.getMonthlyReport(userPrincipal.getUser(), year, month)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/yearly/{year}")
    fun getYearlyReport(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable year: Int
    ): ResponseEntity<YearlyReportResponse> {
        if (year < 1900 || year > 2100) {
            throw BadRequestException("Invalid year")
        }
        val response = reportService.getYearlyReport(userPrincipal.getUser(), year)
        return ResponseEntity.ok(response)
    }
}
