package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

typealias Subst = Map<String, Type>

val emptySubst: Subst = emptyMap()

infix fun <A, B> Map<A, B>.union(map: Map<A, B>) = map + this

infix fun Subst.compose(subst: Subst): Subst = (subst union this).mapValues { apply(this, it.value) }

fun apply(subst: Subst, scheme: Scheme): Scheme = run {
    val substP = scheme.names.foldRight(subst) { a, b -> b - a }
    Scheme(scheme.names, apply(substP, scheme.type))
}

fun apply(subst: Subst, type: Type): Type = when (type) {
    is Type.Const -> type
    is Type.Var -> subst.getOrDefault(type.name, type)
    is Type.Apply -> Type.Apply(apply(subst, type.t1), apply(subst, type.t2))
}

fun apply(subst: Subst, env: Env): Env = env.map { apply(subst, it) }

val Scheme.freeTypeVariables: Set<String> get() = type.freeTypeVariables - names.toSet()

val Type.freeTypeVariables: Set<String> get() = when (this) {
    is Type.Const -> emptySet()
    is Type.Var -> setOf(name)
    is Type.Apply -> t1.freeTypeVariables union t2.freeTypeVariables
}
