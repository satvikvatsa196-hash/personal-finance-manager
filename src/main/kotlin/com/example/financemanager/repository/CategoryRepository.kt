package com.example.financemanager.repository

import com.example.financemanager.model.entity.Category
import com.example.financemanager.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByUserOrUserIsNull(user: User): List<Category>
    fun existsByUserAndName(user: User, name: String): Boolean
    fun findByUserAndName(user: User, name: String): Category?
    fun findByUserIsNullAndName(name: String): Category?

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.name = :name")
    fun findAccessibleCategoryByName(
        @org.springframework.data.repository.query.Param("user") user: User,
        @org.springframework.data.repository.query.Param("name") name: String
    ): Category?
}
