package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.data.*
import arrow.recursion.extensions.fix.recursive.recursive

/**
 * Created by Aedan Smith.
 */

class UnableToUnify(t1: Monotype, t2: Monotype) : Exception("Unable to unify ${t1 to t2}")
class InfiniteBind(tVar: MonotypeF.Variable, type: Monotype) : Exception("Infinite bind ${tVar to type}")

fun unify(a: Monotype, b: Monotype): Subst = run {
    val t1 = a.unfix.fix()
    val t2 = b.unfix.fix()
    when {
        t1 is MonotypeF.Apply && t2 is MonotypeF.Apply -> {
            val s1 = unify(t1.function.value().fix(), t2.function.value().fix())
            val s2 = unify(apply(s1, t1.arg.value().fix()), apply(s1, t2.arg.value().fix()))
            s2 compose s1
        }
        t1 is MonotypeF.Variable -> bind(t1, b)
        t2 is MonotypeF.Variable -> bind(t2, a)
        t1 is MonotypeF.Constant && t2 is MonotypeF.Constant && t1 == t2 -> emptySubst
        else -> throw UnableToUnify(a, b)
    }
}

fun bind(tVar: MonotypeF.Variable, type: Monotype): Subst = when {
    tVar == type.unfix -> emptySubst
    occursIn(tVar, type) -> throw InfiniteBind(tVar, type)
    else -> mapOf(tVar.name to type)
}

fun occursIn(tVar: MonotypeF.Variable, type: Monotype): Boolean = Fix.recursive().run {
    MonotypeFFunctor.cata(type) {
        when (val type = it.fix()) {
            is MonotypeF.Constant -> Eval.now(false)
            is MonotypeF.Variable -> Eval.now(type == tVar)
            is MonotypeF.Apply -> Eval.monad().binding {
                type.function.bind() || type.arg.bind()
            }.fix()
        }
    }
}