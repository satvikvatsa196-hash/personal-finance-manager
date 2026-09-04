package com.example.financemanager.service

import com.example.financemanager.dto.request.GoalRequest
import com.example.financemanager.dto.request.GoalUpdateRequest
import com.example.financemanager.dto.response.GoalDto
import com.example.financemanager.dto.response.GoalListResponse
import com.example.financemanager.exception.ResourceNotFoundException
import com.example.financemanager.model.entity.SavingsGoal
import com.example.financemanager.model.entity.User
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.SavingsGoalRepository
import com.example.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class GoalService(
    private val goalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional
    fun createGoal(user: User, request: GoalRequest): GoalDto {
        val startDate = request.startDate ?: LocalDate.now()

        val goal = SavingsGoal(
            goalName = requireNotNull(request.goalName) { "Goal name is required" },
            targetAmount = requireNotNull(request.targetAmount) { "Target amount is required" },
            startDate = startDate,
            targetDate = requireNotNull(request.targetDate) { "Target date is required" },
            user = user
        )

        val saved = goalRepository.save(goal)
        return mapToDto(user, saved)
    }

    @Transactional(readOnly = true)
    fun getGoals(user: User): GoalListResponse {
        val goals = goalRepository.findByUser(user)
        return GoalListResponse(goals.map { mapToDto(user, it) })
    }

    @Transactional(readOnly = true)
    fun getGoal(user: User, id: Long): GoalDto {
        val goal = getGoalForUser(user, id)
        return mapToDto(user, goal)
    }

    @Transactional
    fun updateGoal(user: User, id: Long, request: GoalUpdateRequest): GoalDto {
        val goal = getGoalForUser(user, id)

        goal.targetAmount = requireNotNull(request.targetAmount) { "Target amount is required" }
        goal.targetDate = requireNotNull(request.targetDate) { "Target date is required" }

        val updated = goalRepository.save(goal)
        return mapToDto(user, updated)
    }

    @Transactional
    fun deleteGoal(user: User, id: Long) {
        val goal = getGoalForUser(user, id)
        goalRepository.delete(goal)
    }

    private fun getGoalForUser(user: User, id: Long): SavingsGoal {
        val goal = goalRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Goal not found") }

        if (goal.user.id != user.id) {
            throw ResourceNotFoundException("Goal not found")
        }

        return goal
    }

    private fun mapToDto(user: User, goal: SavingsGoal): GoalDto {
        val income = transactionRepository.sumAmountByUserAndTypeSinceDate(user, goal.startDate, CategoryType.INCOME) ?: BigDecimal.ZERO
        val expenses = transactionRepository.sumAmountByUserAndTypeSinceDate(user, goal.startDate, CategoryType.EXPENSE) ?: BigDecimal.ZERO

        val currentProgress = income.subtract(expenses)
        val remainingAmount = goal.targetAmount.subtract(currentProgress).coerceAtLeast(BigDecimal.ZERO)

        val progressPercentage = if (goal.targetAmount > BigDecimal.ZERO) {
            currentProgress.divide(goal.targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .min(BigDecimal("100.00")) // Cap at 100%
        } else {
            BigDecimal.ZERO
        }

        return GoalDto(
            id = requireNotNull(goal.id) { "Goal ID should not be null" },
            goalName = goal.goalName,
            targetAmount = goal.targetAmount,
            targetDate = goal.targetDate,
            startDate = goal.startDate,
            currentProgress = currentProgress,
            progressPercentage = progressPercentage,
            remainingAmount = remainingAmount
        )
    }
}
