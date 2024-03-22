package ru.adelvanchik.plugins.database.transaction

import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import ru.adelvanchik.plugins.DatabaseConnection

object Transactions : Table<Nothing>(schema = "bookings", tableName = "transactions") {
    val transactionId = int("transaction_id").primaryKey()
    val passengerId = int("passenger_id")
    val flightId = int("flight_id")
    val isBagIncluded = varchar("is_bag_included")
    val food = varchar("food")
    val amount = int("amount")
    val status = varchar("status")

    private val db = DatabaseConnection.database


    fun getTransaction(transactionId: Int): Transaction? = db.from(Transactions)
        .select()
        .where(Transactions.transactionId eq transactionId)
        .map { row ->
            Transaction(
                transactionId = row[Transactions.transactionId]!!,
                passengerId = row[Transactions.passengerId]!!,
                flightId = row[flightId]!!,
                isBagIncluded = row[isBagIncluded]!!,
                food = row[food]!!,
                amount = row[amount]!!,
                status = row[status]!!,
            )
        }.firstOrNull()

    fun getActiveTransaction(passengerId: Int, flightId: Int): Transaction? = db.from(Transactions)
        .select()
        .where {
            (Transactions.passengerId eq passengerId) and
                    (Transactions.flightId eq flightId) and
                    (Transactions.status eq STATUS.ACTIVE.status)
        }
        .map { row ->
            Transaction(
                transactionId = row[Transactions.transactionId]!!,
                passengerId = row[Transactions.passengerId]!!,
                flightId = row[Transactions.flightId]!!,
                isBagIncluded = row[isBagIncluded]!!,
                food = row[food]!!,
                amount = row[amount]!!,
                status = row[status]!!,
            )
        }.firstOrNull()

    fun getCountActiveTickets(flightId: Int): Int = db.from(Transactions)
        .select()
        .where((Transactions.flightId eq flightId))
        .map { row -> row[status]!! }
        .count { it == STATUS.ACTIVE.status }

    fun insertTransaction(
        passengerId: Int,
        flightId: Int,
        isBagIncluded: String,
        food: String,
        amount: Int,
        status: String,
    ) {
        db.insert(Transactions) {
            set(Transactions.passengerId, passengerId)
            set(Transactions.flightId, flightId)
            set(Transactions.isBagIncluded, isBagIncluded)
            set(Transactions.food, food)
            set(Transactions.amount, amount)
            set(Transactions.status, status)
        }
    }

    fun updateStatus(transactionId: Int, newStatus: STATUS) {
        db.update(Transactions) {
            set(status, newStatus.status)
            where {
                Transactions.transactionId eq transactionId
            }
        }
    }

    enum class STATUS(val status: String) {
        ACTIVE("active"),
        CANCELLED("cancelled"),
    }
}