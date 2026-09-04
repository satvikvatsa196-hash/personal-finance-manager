package com.example.financemanager.config

import com.example.financemanager.model.entity.Category
import com.example.financemanager.model.enums.CategoryType
import com.example.financemanager.repository.CategoryRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional

@Configuration
class DataInitializer(
    private val categoryRepository: CategoryRepository
) {

    @Bean
    @Transactional
    fun initDefaultCategories(): CommandLineRunner {
        return CommandLineRunner {
            val defaults = listOf(
                Pair("Salary", CategoryType.INCOME),
                Pair("Food", CategoryType.EXPENSE),
                Pair("Rent", CategoryType.EXPENSE),
                Pair("Transportation", CategoryType.EXPENSE),
                Pair("Entertainment", CategoryType.EXPENSE),
                Pair("Healthcare", CategoryType.EXPENSE),
                Pair("Utilities", CategoryType.EXPENSE)
            )

            defaults.forEach { (name, type) ->
                if (categoryRepository.findByUserIsNullAndName(name) == null) {
                    categoryRepository.save(
                        Category(
                            name = name,
                            type = type,
                            isCustom = false,
                            user = null
                        )
                    )
                }
            }
        }
    }
}
