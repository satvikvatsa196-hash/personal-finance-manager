package com.example.financemanager.repository

import com.example.financemanager.model.entity.SavingsGoal
import com.example.financemanager.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SavingsGoalRepository : JpaRepository<SavingsGoal, Long> {
    fun findByUser(user: User): List<SavingsGoal>
}
