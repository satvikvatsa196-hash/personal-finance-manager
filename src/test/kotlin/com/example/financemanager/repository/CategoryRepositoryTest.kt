package com.example.financemanager.repository

import com.example.financemanager.model.entity.Category
import com.example.financemanager.model.entity.User
import com.example.financemanager.model.enums.CategoryType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.junit.jupiter.api.assertThrows
import jakarta.persistence.EntityManager

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private lateinit val categoryRepository: CategoryRepository

    @Autowired
    private lateinit val userRepository: UserRepository
    
    @Autowired
    private lateinit val entityManager: EntityManager

    @Test
    fun `should save and find custom category by user`() {
        val user = userRepository.save(User("user1", "hash", "User One", "123"))
        val category = Category(name = "Freelance", type = CategoryType.INCOME, isCustom = true, user = user)
        categoryRepository.save(category)

        val categories = categoryRepository.findByUserOrUserIsNull(user)
        assertThat(categories).hasSize(1)
        assertThat(categories[0].name).isEqualTo("Freelance")
    }

    @Test
    fun `should find default and custom categories for user`() {
        val user = userRepository.save(User("user2", "hash", "User Two", "123"))
        val customCat = Category(name = "Side Hustle", type = CategoryType.INCOME, isCustom = true, user = user)
        val defaultCat = Category(name = "Salary", type = CategoryType.INCOME, isCustom = false, user = null)
        
        categoryRepository.save(customCat)
        categoryRepository.save(defaultCat)

        val categories = categoryRepository.findByUserOrUserIsNull(user)
        assertThat(categories).hasSize(2)
        assertThat(categories.map { it.name }).containsExactlyInAnyOrder("Side Hustle", "Salary")
    }

    @Test
    fun `should enforce unique constraint on user_id and name`() {
        val user = userRepository.save(User("user3", "hash", "User Three", "123"))
        val cat1 = Category(name = "Gifts", type = CategoryType.INCOME, isCustom = true, user = user)
        categoryRepository.saveAndFlush(cat1)

        val cat2 = Category(name = "Gifts", type = CategoryType.EXPENSE, isCustom = true, user = user)
        
        assertThrows<DataIntegrityViolationException> {
            categoryRepository.saveAndFlush(cat2)
        }
    }
}
