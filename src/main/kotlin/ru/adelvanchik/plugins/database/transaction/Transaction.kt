package ru.adelvanchik.plugins.database.transaction

data class Transaction(
    val transactionId: Int,
    val passengerId: Int,
    val flightId: Int,
    val isBagIncluded: String,
    val food: String,
    val amount: Int,
    val status: String,
)
