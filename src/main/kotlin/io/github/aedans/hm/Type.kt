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

data class Scheme(val names: List<String>, val type: Type) {
    override fun toString() = "$names => $type"
}

val Type.Arrow.type get() = Type.Apply(Type.Apply(Type.Const("->"), t1), t2)

val Type.scheme get() = Scheme(emptyList(), this)

fun Type.generalize(env: Env) = Scheme(
        freeTypeVariables.filterNot { tVar -> env.get(tVar).let { it != null && it.names.contains(tVar) } },
        this
)

fun Scheme.instantiate() = run {
    val namesP = names.map { Type.Var(fresh()) }
    val namesZ: Subst = (names zip namesP).toMap()
    apply(namesZ, type)
}
