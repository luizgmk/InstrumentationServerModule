package com.example.instrumentationserver

import kotlin.reflect.KCallable
import kotlin.reflect.jvm.kotlinFunction

class ReflectionHelpers {
    companion object {
        fun explainClass(klass: Any, extended: Boolean = false): Map<String, Any?> {
            val members = klass::class::members.get()
            return mapOf(
                "simpleName" to klass::class.simpleName,
                "members" to listOf(
                    members.map { member ->
                        if (extended)
                            explainClassMember(member)
                        else member.name
                    }
                )
            )
        }

        fun explainClassMember(member: KCallable<*>) = mapOf(
            "name" to member.name,
            "isAbstract" to member.isAbstract,
            "isFinal" to member.isFinal,
            "isOpen" to member.isOpen,
            "isSuspend" to member.isSuspend,
            "parameters" to member.parameters.drop(1).map { p ->
                mapOf(
                    "name" to (p.name ?: "null"),
                    "isVararg" to p.isVararg,
                    "type" to p.type.toString()
                )
            }
        )

        // Source: inspired on Gemini inputs
        fun getMethodSignatures(klass: Class<*>): Map<String, Any?> {
            return mapOf(
                "name" to klass.simpleName,
                "constructors" to klass.constructors.map { method ->
                    "${method.name}(${method.parameters.joinToString { it.type.toString() }}): ${method.kotlinFunction?.returnType}"
                },
                "methods" to klass.methods.map { method ->
                    "${method.name}(${method.parameters.joinToString { it.type.toString() }}): ${method.returnType}"
                }
            )
        }
    }
}