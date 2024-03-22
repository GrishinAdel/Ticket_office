package ru.adelvanchik.plugins.kassa

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.adelvanchik.plugins.database.flight.Flights
import ru.adelvanchik.plugins.database.passenger.Passengers
import ru.adelvanchik.plugins.database.transaction.Transactions
import ru.adelvanchik.plugins.kassa.Errors.INVALID_PASSENGER
import ru.adelvanchik.plugins.kassa.Errors.INVALID_PASSPORT
import ru.adelvanchik.plugins.kassa.Errors.INVALID_TRANSACTION
import ru.adelvanchik.plugins.kassa.Errors.NOT_SUITABLE_FLIGHTS
import ru.adelvanchik.plugins.kassa.Errors.PASSENGER_ALREADY_HAVE_ACTIVE_TICKET
import ru.adelvanchik.plugins.kassa.Errors.TICKET_CANNOT_REFUNDED
import ru.adelvanchik.plugins.kassa.Errors.TRANSACTION_NOT_ACTIVE
import ru.adelvanchik.plugins.kassa.Errors.UNKNOWN_ERROR
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Application.ticketsRouting() {
    routing {
        post("buy_ticket") {
            val data = try {
                call.receive<BuyTicketRequest>()
            } catch (e: Exception) {
                null
            }

            println("Get data for buy_ticket:")
            println(data.toString())

            if (data == null) {
                call.respond(HttpStatusCode.BadRequest, Errors.PARSING_ERROR)
                return@post
            }

            when {
                data.fio.isNotCorrectFIOFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_FIO_FORMAT)
                    return@post
                }

                data.passport.isNotCorrectPassportFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_PASSPORT_FORMAT)
                    return@post
                }

                data.dateOfBirth.isNotValidBirthdayFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_DATE_OF_BIRTH_FORMAT)
                    return@post
                }

                data.dateOfBirth.isYoungerPassenger() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.YOUNG_PASSENGER)
                    return@post
                }

                data.bag.isNotCorrectBagFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_BAG_FORMAT)
                    return@post
                }

                data.food.isNotCorrectFoodFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_FOOD_FORMAT)
                    return@post
                }

                data.where.isNotCorrectCity() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_CITY_FORMAT)
                    return@post
                }
            }

            val flight = Flights.getIdFlightForBuyTicket(city = data.where)

            if (flight == null) {
                call.respond(HttpStatusCode.BadRequest, NOT_SUITABLE_FLIGHTS)
                return@post
            }

            var passenger = Passengers.getPassenger(passport = data.passport)

            if (passenger != null && (passenger.fio != data.fio || passenger.birth != data.dateOfBirth)) {
                call.respond(HttpStatusCode.BadRequest, INVALID_PASSPORT)
                return@post
            }

            if (passenger == null) {
                Passengers.insertPassenger(
                    fio = data.fio,
                    passport = data.passport,
                    birth = data.dateOfBirth,
                )
                passenger = Passengers.getPassenger(passport = data.passport)
            }

            if (passenger == null) {
                call.respond(HttpStatusCode.BadRequest, UNKNOWN_ERROR)
                return@post
            }

            var transaction = Transactions.getActiveTransaction(
                passengerId = passenger.id,
                flightId = flight.id,
            )

            if (transaction != null) {
                call.respond(HttpStatusCode.BadRequest, PASSENGER_ALREADY_HAVE_ACTIVE_TICKET)
                return@post
            }

            val isBagIncluded = data.bag == BAG_INCLUDE
            val amount = if (isBagIncluded) flight.price * COEFFICIENT_APPRECIATION_WITH_BAG else flight.price

            Transactions.insertTransaction(
                passengerId = passenger.id,
                flightId = flight.id,
                isBagIncluded = data.bag.toString(),
                food = data.food,
                amount = amount.toInt(),
                status = Transactions.STATUS.ACTIVE.status,
            )

            transaction = Transactions.getActiveTransaction(
                passengerId = passenger.id,
                flightId = flight.id,
            )

            if (transaction == null) {
                call.respond(HttpStatusCode.BadRequest, UNKNOWN_ERROR)
                return@post
            }

            val answer = "${transaction.amount}_${transaction.transactionId}"
            println("Respond answer for buy_ticket:")
            println(answer)

            call.respond(HttpStatusCode.OK, answer)
        }

        post("refund_ticket") {
            val data = try {
                call.receive<RefundTicketRequest>()
            } catch (e: Exception) {
                null
            }

            println("Get data for refund_ticket:")
            println(data.toString())

            if (data == null) {
                call.respond(HttpStatusCode.BadRequest, Errors.PARSING_ERROR)
                return@post
            }

            when {
                data.fio.isNotCorrectFIOFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_FIO_FORMAT)
                    return@post
                }

                data.passport.isNotCorrectPassportFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_PASSPORT_FORMAT)
                    return@post
                }

                data.dateOfBirth.isNotValidBirthdayFormat() -> {
                    call.respond(HttpStatusCode.BadRequest, Errors.NOT_CORRECT_DATE_OF_BIRTH_FORMAT)
                    return@post
                }
            }

            val passenger = Passengers.getPassenger(passport = data.passport)

            if (passenger == null || passenger.birth != data.dateOfBirth || passenger.fio != data.fio) {
                call.respond(HttpStatusCode.BadRequest, INVALID_PASSENGER)
                return@post
            }

            val transaction = Transactions.getTransaction(transactionId = data.idTransaction)

            if (transaction == null) {
                call.respond(HttpStatusCode.BadRequest, INVALID_TRANSACTION)
                return@post
            }

            if (transaction.status != Transactions.STATUS.ACTIVE.status) {
                call.respond(HttpStatusCode.BadRequest, TRANSACTION_NOT_ACTIVE)
                return@post
            }

            val isCanRefundTicket = Flights.isCanRefundTicket(transaction.flightId)

            if (isCanRefundTicket != true) {
                call.respond(HttpStatusCode.BadRequest, TICKET_CANNOT_REFUNDED)
                return@post
            }

            Transactions.updateStatus(transactionId = data.idTransaction, Transactions.STATUS.CANCELLED)

            val answer = transaction.amount
            println("Respond answer for refund_ticket:")
            println(answer)

            call.respond(HttpStatusCode.OK, answer)
        }
    }
}

fun String.isNotCorrectFIOFormat(): Boolean {
    val goodSymbols = "abcdefghijklmnopqrstuvwxyz"
    val credentials = this.split(" ")
    if (credentials.size != 2) return true
    for (cred in credentials) {
        for (symbol in cred) {
            if (symbol.lowercase() !in goodSymbols) {
                return true
            }
        }
    }
    return false
}

fun Long.isNotCorrectPassportFormat(): Boolean = this !in 10_00_000000..99_99_999999

fun String.isNotValidBirthdayFormat(): Boolean {
    val birthdayRegex = Regex("^\\d{2}\\/\\d{2}\\/\\d{4}$")
    val birthdayRegex2 = Regex("^\\d{1}\\/\\d{2}\\/\\d{4}$")
    if (!birthdayRegex.matches(this) && !birthdayRegex2.matches(this)) {
        return true
    }
    return false
}

fun String.isYoungerPassenger(): Boolean {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val birthdayDate = LocalDate.parse(this, formatter)
    val currentDate = LocalDate.now()

    if (birthdayDate.plusYears(12) > currentDate) {
        return true
    }

    return false
}

fun Char.isNotCorrectBagFormat(): Boolean = this != BAG_NOT_INCLUDE && this != BAG_INCLUDE

fun String.isNotCorrectFoodFormat(): Boolean = this != "fish" && this != "meat"

fun String.isNotCorrectCity(): Boolean = !cities.contains(this)

val cities = listOf(
    "TJM",
    "NYA",
    "UKX",
    "GDZ",
    "NFG",
    "VKT",
    "MMK",
    "BAX",
    "UUD",
    "BQS",
    "HTA",
    "KUF",
    "CEE",
    "MCX",
    "ADL"
)

object Errors {
    const val PARSING_ERROR = "parsing_error"
    const val NOT_CORRECT_FIO_FORMAT = "not_correct_fio_format"
    const val NOT_CORRECT_PASSPORT_FORMAT = "not_correct_passport_format"
    const val NOT_CORRECT_DATE_OF_BIRTH_FORMAT = "not_correct_date_of_birth_format"
    const val NOT_CORRECT_BAG_FORMAT = "not_correct_bag_format"
    const val NOT_CORRECT_FOOD_FORMAT = "not_correct_food_format"
    const val NOT_CORRECT_CITY_FORMAT = "not_correct_city_name"
    const val YOUNG_PASSENGER = "young_passenger"
    const val NOT_SUITABLE_FLIGHTS = "not_suitable_flights"
    const val PASSENGER_ALREADY_HAVE_ACTIVE_TICKET = "passenger_already_have_active_ticket"
    const val INVALID_PASSPORT = "passenger_passport_data_using_for_another_passenger"
    const val INVALID_PASSENGER = "this_invalid_passenger"
    const val INVALID_TRANSACTION = "this_invalid_transaction"
    const val TRANSACTION_NOT_ACTIVE = "this_transaction_not_active"
    const val TICKET_CANNOT_REFUNDED = "ticket_cant_be_refunded"
    const val UNKNOWN_ERROR = "unknown_error"
}

private val BAG_INCLUDE = '1'
private val BAG_NOT_INCLUDE = '0'

private val COEFFICIENT_APPRECIATION_WITH_BAG = 1.2f