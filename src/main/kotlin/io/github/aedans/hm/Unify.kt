package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun unify(t1: Type, t2: Type): Subst = when {
    t1 is Type.Arrow && t2 is Type.Arrow -> {
        val s1 = unify(t1.t1, t2.t1)
        val s2 = unify(apply(s1, t1.t2), apply(s1, t2.t2))
        s2 compose s1
    }
    t1 is Type.Var -> bind(t1, t2)
    t2 is Type.Var -> bind(t2, t1)
    t1 is Type.Const && t2 is Type.Const && t1 == t2 -> emptyMap()
    else -> throw Exception("Unable to unify")
}

fun bind(tVar: Type.Var, type: Type): Subst = when {
    tVar == type -> emptyMap()
    occursIn(tVar, type) -> throw Exception("Infinite")
    else -> mapOf(tVar to type)
}

fun occursIn(tVar: Type.Var, type: Type): Boolean = when (type) {
    is Type.Arrow -> occursIn(tVar, type.t1) || occursIn(tVar, type.t2)
    is Type.Var -> type == tVar
    is Type.Const -> false
}
