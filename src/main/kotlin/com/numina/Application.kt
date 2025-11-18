package com.numina

import com.numina.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureDatabase()
    configureSerialization()
    configureSecurity()
    configureRouting()
}
