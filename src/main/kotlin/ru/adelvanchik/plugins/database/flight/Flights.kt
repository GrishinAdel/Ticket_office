package ru.adelvanchik.plugins.database.flight

import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar
import ru.adelvanchik.plugins.DatabaseConnection
import ru.adelvanchik.plugins.database.passenger.Passengers
import ru.adelvanchik.plugins.database.transaction.Transactions

object Flights : Table<Nothing>(schema = "bookings", tableName = "flights") {
    val flightId = int("flight_id").primaryKey()
    val scheduledArrival = timestamp("scheduled_arrival")
    val arrivalAirport = varchar("arrival_airport")
    val status = varchar("status")
    val price = int("price")

    private val db = DatabaseConnection.database

    fun getIdFlightForBuyTicket(city: String): FlightIdAmount? = db.from(Flights)
        .select()
        .where(Flights.arrivalAirport eq city)
        .map { row ->
            Flight(
                id = row[Flights.flightId]!!,
                scheduledDeparture = row[Flights.scheduledArrival]!!.epochSecond,
                status = row[Flights.status]!!,
                price = row[Flights.price]!!,
            )
        }
        .filter { it.status.lowercase().isCanBuyTicketWithStatus() && Transactions.getCountActiveTickets(it.id) < MAX_ACTIVE_TICKETS }
        .sortedBy { it.scheduledDeparture }
        .map {
            FlightIdAmount(
                id = it.id,
                price = it.price
            )
        }
        .firstOrNull()

    fun isCanRefundTicket(id: Int): Boolean? = db.from(Flights)
        .select()
        .where(Flights.flightId eq id)
        .map { row -> row[Flights.status]!! }
        .filter { it.lowercase().isCanRefundTicketWithStatus() }
        .map { true }
        .firstOrNull()


    private fun String.isCanBuyTicketWithStatus(): Boolean =
        this == "scheduled" || this == "on time" || this == "delayed" || this == "cancelled"

    private fun String.isCanRefundTicketWithStatus(): Boolean = this == "cancelled"

    private val MAX_ACTIVE_TICKETS = 200
}