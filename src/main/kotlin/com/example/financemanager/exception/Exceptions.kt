package com.example.financemanager.exception

open class FinanceManagerException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : FinanceManagerException(message)

class UnauthorizedException(message: String) : FinanceManagerException(message)

class ForbiddenException(message: String) : FinanceManagerException(message)

class ResourceNotFoundException(message: String) : FinanceManagerException(message)

class ConflictException(message: String) : FinanceManagerException(message)
