package com.example.financemanager.controller

import com.example.financemanager.dto.request.GoalRequest
import com.example.financemanager.dto.request.GoalUpdateRequest
import com.example.financemanager.dto.response.GoalDto
import com.example.financemanager.dto.response.GoalListResponse
import com.example.financemanager.dto.response.MessageResponse
import com.example.financemanager.security.UserPrincipal
import com.example.financemanager.service.GoalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/goals")
class GoalController(
    private val goalService: GoalService
) {

    @PostMapping
    fun createGoal(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: GoalRequest
    ): ResponseEntity<GoalDto> {
        val response = goalService.createGoal(userPrincipal.getUser(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getGoals(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<GoalListResponse> {
        val response = goalService.getGoals(userPrincipal.getUser())
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getGoal(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<GoalDto> {
        val response = goalService.getGoal(userPrincipal.getUser(), id)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    fun updateGoal(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: GoalUpdateRequest
    ): ResponseEntity<GoalDto> {
        val response = goalService.updateGoal(userPrincipal.getUser(), id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun deleteGoal(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<MessageResponse> {
        goalService.deleteGoal(userPrincipal.getUser(), id)
        return ResponseEntity.ok(MessageResponse("Goal deleted successfully"))
    }
}
