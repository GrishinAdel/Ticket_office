package ru.adelvanchik

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import ru.adelvanchik.plugins.*

fun main() {
    DatabaseConnection.database

    embeddedServer(
        Netty,
        port = 8080,
        host = "192.168.117.16",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    configureRouting()
}
