package ru.adelvanchik.plugins.kassa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RefundTicketRequest(
    val fio: String,
    val passport: Long,
    @SerialName("date_of_birth") val dateOfBirth: String,
    @SerialName("id_transaction")val idTransaction: Int,
)