package com.miaobi.app.domain.exception

/**
 * 应用统一异常类
 */
sealed class AppException(message: String) : Exception(message) {
    class NetworkException(message: String) : AppException(message)
    class ApiKeyException(message: String) : AppException(message)
    class ModelException(message: String) : AppException(message)
    class DatabaseException(message: String) : AppException(message)
}
