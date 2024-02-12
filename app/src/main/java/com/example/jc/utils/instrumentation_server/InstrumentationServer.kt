package com.example.jc.utils.instrumentation_server

import com.example.jc.utils.instrumentation_server.GsonHelper.Companion.gson
import com.example.jc.utils.instrumentation_server.KtorHelpers.Companion.respondWithException
import com.google.gson.internal.LinkedTreeMap
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility

class InstrumentationServer {
    private val memberExclusions = listOf("equals", "hashCode", "toString")
    private val singletonsAttached = mutableListOf<Any>()
    private val classesReferences = mutableListOf<KClass<*>>()

    fun addSingleton(c: Any) {
        if (singletonsAttached.none { i -> i::class == c::class })
            singletonsAttached.add(c)
    }

    fun addClass(c: KClass<*>) {
        classesReferences.add(c)
    }

    fun start(port: Int) {
        val engine = embeddedServer(Netty, port = port) {
            debugLog("Initializing")

            routing {

                get("/explain/class/{className}") {
                    try {
                        val klassName = call.parameters["className"]!!
                        val klass = Class.forName(klassName)
                        val response = ReflectionHelpers.getMethodSignatures(klass)
                        call.respond(HttpStatusCode.OK, gson.toJson(response))
                    } catch (e: Exception) {
                        respondWithException(call, e)
                    }
                }

                get("/info") {
                    val response =
                        singletonsAttached.map { klass -> ReflectionHelpers.explainClass(klass) }
                    call.respond(HttpStatusCode.OK, gson.toJson(response))
                }

                get("/info/extended") {
                    val response = singletonsAttached.map { klass ->
                        ReflectionHelpers.explainClass(klass, true)
                    }
                    call.respond(HttpStatusCode.OK, gson.toJson(response))
                }

                post("/invokeClass") {
                    try {
                        val response =
                            invokeRecursively(gson.fromJson(call.receiveText(), Map::class.java))
                        call.respond(HttpStatusCode.OK, gson.toJson(response))
                    } catch (e: Exception) {
                        respondWithException(call, e)
                    }
                }

                singletonsAttached.forEach { klass ->
                    val c = klass::class
                    val members = c.members.filter { m ->
                        m.visibility == KVisibility.PUBLIC && !memberExclusions.contains(m.name)
                    }

                    get("${c.simpleName}/info") {
                        val response =
                            members.map { member -> ReflectionHelpers.explainClassMember(member) }
                        call.respondText(gson.toJson(response))
                    }
                    members.forEach { member ->
                        debugLog("Setting endpoints for ${c.simpleName}::${member.name}")

                        get("${c.simpleName}/${member.name}/info") {
                            val memberInfo = ReflectionHelpers.explainClassMember(member)
                            call.respondText(gson.toJson(memberInfo))
                        }
                        post("${c.simpleName}/${member.name}") {
                            debugLog("POST received for ${c.simpleName}::${member.name}")
                            try {
                                val data = call.receiveText()
                                var response = if (data.isNotEmpty()) {
                                    val parameters = invokeRecursively(
                                        gson.fromJson(data, Map::class.java)
                                    )
                                    val args = parameters.toTypedArray()
                                    if (member.isSuspend)
                                        suspendCoroutine { continuation ->
                                            member.call(klass, *args, continuation)
                                        }
                                    else
                                        member.call(klass, *args)
                                } else {
                                    member.call(klass)
                                }
                                if (response == null)
                                    call.respond(HttpStatusCode.OK)
                                else {
                                    if ((response is Flow<*>)) {
                                        // TODO: LEAK!! this flow is never completing... fix this
                                        response
                                            .onStart {
                                                debugLog("Flow collection started (FlowCollector hash: ${this.hashCode()})")
                                            }
                                            .onCompletion {
                                                debugLog("Flow collection completed (FlowCollector hash: ${this.hashCode()})")
                                            }
                                            .collectLatest {
                                                call.respond(
                                                    HttpStatusCode.OK,
                                                    gson.toJson(it)
                                                )
                                            }
                                    } else
                                        call.respond(HttpStatusCode.OK, gson.toJson(response))
                                }
                            } catch (e: Exception) {
                                respondWithException(call, e)
                            }
                        }
                    }
                }
            }
        }
        debugLog("Starting on port $port")
        engine.start(wait = true)
        debugLog("Server terminated")
    }

    private fun debugLog(message: String) {
        Timber.d("Instrumentation Server :: $message")
    }
    
    private fun invokeRecursively(data: Map<*, *>): List<Any> =
        data.entries.mapNotNull { parameter ->
            if (parameter.value is Map<*, *>) {
                val klassName = parameter.key
                val klass = classesReferences
                    .find { it.java.name == klassName } ?: return@mapNotNull null
                val complexParam = parameter.value as LinkedTreeMap<*, *>
                val parameters = invokeRecursively(complexParam.toMap())
                klass.constructors.first().call(*parameters.toTypedArray())
            } else parameter.value
        }

}