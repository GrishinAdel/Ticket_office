package ru.adelvanchik.plugins.database.passenger

import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import ru.adelvanchik.plugins.DatabaseConnection.database

object Passengers : Table<Nothing>(schema = "bookings", tableName = "passangers") {
    val passengerId = int("passenger_id").primaryKey()
    val name = varchar("name")
    val passport = varchar("passport")
    val birth = varchar("date_of_birth")

    private val db = database

    fun getPassenger(passport: Long): Passenger? = db.from(Passengers)
        .select()
        .where (Passengers.passport eq passport.toString())
        .map { row ->
            Passenger(
                id = row[passengerId]!!,
                fio = row[name]!!,
                passport = row[Passengers.passport]!!,
                birth = row[Passengers.birth]!!,
            )
        }.firstOrNull()

    fun insertPassenger(fio: String, passport: Long, birth: String) {
        db.insert(Passengers) {
            set(Passengers.name, fio)
            set(Passengers.passport, passport.toString())
            set(Passengers.birth, birth)
        }
    }
}