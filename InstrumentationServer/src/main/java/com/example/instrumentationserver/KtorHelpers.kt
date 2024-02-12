package com.example.instrumentationserver

import com.example.instrumentationserver.GsonHelper.Companion.gson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class KtorHelpers {
    companion object {
        suspend fun responseBadRequest(call: ApplicationCall, message: String) {
            call.respond(
                HttpStatusCode.BadRequest,
                gson.toJson(message)
            )
        }

        suspend fun respondWithException(call: ApplicationCall, e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError, gson.toJson(
                    mapOf(
                        "exception" to e::class.java.name,
                        "message" to e.message
                    )
                )
            )
        }
    }

}