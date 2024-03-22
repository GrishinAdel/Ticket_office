package ru.adelvanchik.plugins.database.flight

data class Flight(
    val id: Int,
    val scheduledDeparture: Long,
    val status: String,
    val price: Int,
)