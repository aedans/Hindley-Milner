package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

sealed class Type {
    data class Var(val name: String) : Type() {
        override fun toString() = name
    }

    data class Const(val name: String) : Type() {
        override fun toString() = name
    }

    data class Arrow(val t1: Type, val t2: Type) : Type() {
        override fun toString() = "($t1 -> $t2)"
    }
}

val Type.scheme get() = Scheme(emptyList(), this)

data class Scheme(val abs: List<Type.Var>, val type: Type) {
    override fun toString() = "$abs => $type"
}
