package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.data.*
import arrow.recursion.extensions.fix.recursive.recursive

/**
 * Created by Aedan Smith.
 */

class UnableToUnify(t1: TLCMonotype, t2: TLCMonotype) : Exception("Unable to unify ${t1 to t2}")
class InfiniteBind(tVar: TLCMonotypeF.Variable, type: TLCMonotype) : Exception("Infinite bind ${tVar to type}")

fun unify(a: TLCMonotype, b: TLCMonotype): Subst = run {
    val t1 = a.unfix.fix()
    val t2 = b.unfix.fix()
    when {
        t1 is TLCMonotypeF.Apply && t2 is TLCMonotypeF.Apply -> {
            val s1 = unify(t1.function.value().fix(), t2.function.value().fix())
            val s2 = unify(apply(s1, t1.arg.value().fix()), apply(s1, t2.arg.value().fix()))
            s2 compose s1
        }
        t1 is TLCMonotypeF.Variable -> bind(t1, b)
        t2 is TLCMonotypeF.Variable -> bind(t2, a)
        t1 is TLCMonotypeF.Constant && t2 is TLCMonotypeF.Constant && t1 == t2 -> emptySubst
        else -> throw UnableToUnify(a, b)
    }
}

fun bind(tVar: TLCMonotypeF.Variable, type: TLCMonotype): Subst = when {
    tVar == type.unfix -> emptySubst
    occursIn(tVar, type) -> throw InfiniteBind(tVar, type)
    else -> mapOf(tVar.name to type)
}

fun occursIn(tVar: TLCMonotypeF.Variable, type: TLCMonotype): Boolean = Fix.recursive().run {
    TLCMonotypeFFunctor.cata(type) {
        when (val type = it.fix()) {
            is TLCMonotypeF.Constant -> Eval.now(false)
            is TLCMonotypeF.Variable -> Eval.now(type == tVar)
            is TLCMonotypeF.Apply -> Eval.monad().binding {
                type.function.bind() || type.arg.bind()
            }.fix()
        }
    }
}