package ru.adelvanchik.plugins

import org.ktorm.database.Database

private const val IP = "5.35.85.29"
private const val PORT = 5432

object DatabaseConnection {
    val database = Database.connect(
        url = "jdbc:postgresql://$IP:$PORT/demo",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "air5L(8L02)mz",
    )

}