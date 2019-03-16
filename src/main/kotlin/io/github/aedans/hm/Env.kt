package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

var fresh = 0
fun fresh() = "${fresh++}"

interface Env {
    fun get(name: String): TLCType.Poly?

    companion object {
        val empty = object : Env {
            override fun get(name: String) = null
            override fun toString() = "[]"
        }
    }
}

fun Env.set(name: String, poly: TLCType.Poly): Env = run {
    @Suppress("UnnecessaryVariable", "LocalVariableName")
    val _name = name
    object : Env {
        override fun get(name: String) = if (name == _name) poly else this@set.get(name)
    }
}

fun Env.map(fn: (TLCType.Poly) -> TLCType.Poly) = object : Env {
    override fun get(name: String) = this@map.get(name)?.let(fn)
}
