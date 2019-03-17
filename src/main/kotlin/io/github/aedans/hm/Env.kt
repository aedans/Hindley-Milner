package io.github.aedans.hm

import arrow.syntax.collections.prependTo

/**
 * An environment for polytypes.
 */
data class Env<out T>(val reserved: Set<String>, val map: Map<String, Polytype<T>>) {
    operator fun get(name: String) = map[name]

    companion object {
        val empty = Env<Nothing>(emptySet(), emptyMap())
    }
}

fun <T> Env<T>.reserve(name: String): Env<T> =
        copy(reserved = reserved + name)

fun <T> Env<T>.put(name: String, poly: Polytype<T>): Env<T> =
        copy(map = map + (name to poly)).reserve(name)

fun <T> Env<T>.map(fn: (Polytype<T>) -> Polytype<T>): Env<T> =
        copy(map = map.mapValues { (_, it) -> fn(it) })

fun <T> Env<T>.fresh(char: Char = 'a'): Pair<Char, Env<T>> = when {
    reserved.contains(char.toString()) -> fresh(char + 1)
    else -> char to reserve(char.toString())
}

fun <T> Env<T>.fresh(i: Int, char: Char = 'a'): Pair<List<Char>, Env<T>> = when (i) {
    0 -> emptyList<Char>() to this
    else -> {
        val (fresh, env) = fresh(char)
        val (list, env2) = env.fresh(i - 1, fresh)
        fresh.prependTo(list) to env2
    }
}