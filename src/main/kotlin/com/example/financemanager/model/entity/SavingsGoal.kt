package com.example.financemanager.model.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "savings_goals")
class SavingsGoal(
    @Column(name = "goal_name", nullable = false)
    var goalName: String,

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    var targetAmount: BigDecimal,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "target_date", nullable = false)
    var targetDate: LocalDate,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SavingsGoal
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
