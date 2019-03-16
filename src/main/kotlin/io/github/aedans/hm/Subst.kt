package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

typealias Subst = Map<String, TLCType.Mono>

val emptySubst: Subst = emptyMap()

infix fun <A, B> Map<A, B>.union(map: Map<A, B>) = map + this

infix fun Subst.compose(subst: Subst) = (subst union this).mapValues { apply(this, it.value) }

fun apply(subst: Subst, poly: TLCType.Poly) = run {
    val substP = poly.names.foldRight(subst) { a, b -> b - a }
    TLCType.Poly(poly.names, apply(substP, poly.type))
}

fun apply(subst: Subst, type: TLCType.Mono): TLCType.Mono = when (type) {
    is TLCType.Mono.Const -> type
    is TLCType.Mono.Var -> subst.getOrDefault(type.name, type)
    is TLCType.Mono.Apply -> TLCType.Mono.Apply(apply(subst, type.function), apply(subst, type.arg))
}

fun apply(subst: Subst, env: Env) = env.map { apply(subst, it) }

val TLCType.Poly.freeTypeVariables get() = type.freeTypeVariables - names.toSet()

val TLCType.Mono.freeTypeVariables: Set<String> get() = when (this) {
    is TLCType.Mono.Const -> emptySet()
    is TLCType.Mono.Var -> setOf(name)
    is TLCType.Mono.Apply -> function.freeTypeVariables union arg.freeTypeVariables
}
