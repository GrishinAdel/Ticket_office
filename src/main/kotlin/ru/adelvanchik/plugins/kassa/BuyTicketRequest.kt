package ru.adelvanchik.plugins.kassa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuyTicketRequest(
    val fio: String,
    val passport: Long,
    @SerialName("date_of_birth") val dateOfBirth: String,
    val bag: Char,
    val food: String,
    val where: String,
)

