package com.example.financemanager.model.entity

import com.example.financemanager.model.enums.CategoryType
import jakarta.persistence.*

@Entity
@Table(
    name = "categories",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "name"])
    ]
)
class Category(
    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: CategoryType,

    @Column(name = "is_custom", nullable = false)
    var isCustom: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null // Null if it's a default category
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Category
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
