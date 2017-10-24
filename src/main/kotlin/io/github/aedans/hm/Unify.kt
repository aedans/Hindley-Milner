package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

class UnableToUnify(t1: Type, t2: Type) : Exception("Unable to unify ${t1 to t2}")
class InfiniteBind(tVar: Type.Var, type: Type) : Exception("Infinite bind ${tVar to type}")

fun unify(t1: Type, t2: Type): Subst = when {
    t1 is Type.Apply && t2 is Type.Apply -> {
        val s1 = unify(t1.t1, t2.t1)
        val s2 = unify(apply(s1, t1.t2), apply(s1, t2.t2))
        s2 compose s1
    }
    t1 is Type.Var -> bind(t1, t2)
    t2 is Type.Var -> bind(t2, t1)
    t1 is Type.Const && t2 is Type.Const && t1 == t2 -> emptySubst
    else -> throw UnableToUnify(t1, t2)
}

fun bind(tVar: Type.Var, type: Type): Subst = when {
    tVar == type -> emptySubst
    occursIn(tVar, type) -> throw InfiniteBind(tVar, type)
    else -> mapOf(tVar.name to type)
}

fun occursIn(tVar: Type.Var, type: Type): Boolean = when (type) {
    is Type.Apply -> occursIn(tVar, type.t1) || occursIn(tVar, type.t2)
    is Type.Var -> type == tVar
    is Type.Const -> false
}
