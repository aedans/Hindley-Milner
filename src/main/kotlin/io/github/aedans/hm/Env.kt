package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

var fresh = 0
fun fresh() = "${fresh++}"

interface Env {
    fun get(name: String): Scheme?

    companion object {
        val empty = object : Env {
            override fun get(name: String) = null
            override fun toString() = "[]"
        }
    }
}

fun Env.set(name: String, scheme: Scheme): Env = run {
    @Suppress("UnnecessaryVariable", "LocalVariableName")
    val _name = name
    object : Env {
        override fun get(name: String) = if (name == _name) scheme else this@set.get(name)
    }
}

fun Env.map(fn: (Scheme) -> Scheme) = object : Env {
    override fun get(name: String) = this@map.get(name)?.let(fn)
}
