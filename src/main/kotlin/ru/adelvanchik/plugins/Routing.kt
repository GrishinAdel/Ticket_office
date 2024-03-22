package ru.adelvanchik.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.adelvanchik.plugins.kassa.ticketsRouting
import java.util.UUID


fun Application.configureRouting() {
    ticketsRouting()
}


