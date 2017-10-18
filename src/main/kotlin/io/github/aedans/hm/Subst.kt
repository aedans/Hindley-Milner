package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

typealias Subst = Map<Type.Var, Type>

val emptySubst: Subst = emptyMap()

infix fun <A, B> Map<A, B>.union(map: Map<A, B>) = map + this

infix fun Subst.compose(subst: Subst): Subst = (subst union this).mapValues { apply(this, it.value) }

fun apply(subst: Subst, scheme: Scheme): Scheme = run {
    val substP = scheme.abs.foldRight(subst) { a, b -> b - a }
    Scheme(scheme.abs, apply(substP, scheme.type))
}

fun apply(subst: Subst, type: Type): Type = when (type) {
    is Type.Const -> type
    is Type.Var -> subst.getOrDefault(type, type)
    is Type.Arrow -> Type.Arrow(apply(subst, type.t1), apply(subst, type.t2))
}

fun apply(subst: Subst, env: Env): Env = env.mapValues { apply(subst, it.value) }

val Scheme.freeTypeVariables: Set<Type.Var> get() = type.freeTypeVariables - abs.toSet()

val Type.freeTypeVariables: Set<Type.Var> get() = when (this) {
    is Type.Const -> emptySet()
    is Type.Var -> setOf(this)
    is Type.Arrow -> t1.freeTypeVariables union t2.freeTypeVariables
}

val Env.freeTypeVariables: Set<Type.Var> get() = entries.flatMap { it.value.freeTypeVariables }.toSet()
