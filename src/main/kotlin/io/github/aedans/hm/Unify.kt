package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

class UnableToUnify(t1: TLCType.Mono, t2: TLCType.Mono) : Exception("Unable to unify ${t1 to t2}")
class InfiniteBind(tVar: TLCType.Mono.Var, type: TLCType.Mono) : Exception("Infinite bind ${tVar to type}")

fun unify(t1: TLCType.Mono, t2: TLCType.Mono): Subst = when {
    t1 is TLCType.Mono.Apply && t2 is TLCType.Mono.Apply -> {
        val s1 = unify(t1.function, t2.function)
        val s2 = unify(apply(s1, t1.arg), apply(s1, t2.arg))
        s2 compose s1
    }
    t1 is TLCType.Mono.Var -> bind(t1, t2)
    t2 is TLCType.Mono.Var -> bind(t2, t1)
    t1 is TLCType.Mono.Const && t2 is TLCType.Mono.Const && t1 == t2 -> emptySubst
    else -> throw UnableToUnify(t1, t2)
}

fun bind(tVar: TLCType.Mono.Var, type: TLCType.Mono): Subst = when {
    tVar == type -> emptySubst
    occursIn(tVar, type) -> throw InfiniteBind(tVar, type)
    else -> mapOf(tVar.name to type)
}

fun occursIn(tVar: TLCType.Mono.Var, type: TLCType.Mono): Boolean = when (type) {
    is TLCType.Mono.Apply -> occursIn(tVar, type.function) || occursIn(tVar, type.arg)
    is TLCType.Mono.Var -> type == tVar
    is TLCType.Mono.Const -> false
}
