package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

var fresh = 0
fun fresh() = "${fresh++}"

interface Env<out T> {
    fun get(name: String): Polytype<T>?

    companion object {
        val empty = object : Env<Nothing> {
            override fun get(name: String) = null
            override fun toString() = "[]"
        }
    }
}

fun <T> Env<T>.set(name: String, poly: Polytype<T>): Env<T> = run {
    @Suppress("UnnecessaryVariable", "LocalVariableName")
    val _name = name
    object : Env<T> {
        override fun get(name: String) = if (name == _name) poly else this@set.get(name)
    }
}

fun <T> Env<T>.map(fn: (Polytype<T>) -> Polytype<T>) = object : Env<T> {
    override fun get(name: String) = this@map.get(name)?.let(fn)
}