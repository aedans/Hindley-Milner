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

    data class Apply(val t1: Type, val t2: Type) : Type() {
        override fun toString() = "($t1) $t2"
    }

    data class Arrow(val t1: Type, val t2: Type)
}

val Type.Arrow.type get() = Type.Apply(Type.Apply(Type.Const("->"), t1), t2)

val Type.scheme get() = Scheme(emptyList(), this)

data class Scheme(val names: List<String>, val type: Type) {
    override fun toString() = "$names => $type"
}
