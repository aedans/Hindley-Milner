package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.extensions.fix.recursive.recursive

/**
 * Created by Aedan Smith.
 */

typealias Subst = Map<String, TLCMonotype>

val emptySubst: Subst = emptyMap()

infix fun Subst.compose(subst: Subst) = (subst + this).mapValues { apply(this, it.value) }

fun apply(subst: Subst, poly: TLCPolytype) = run {
    val substP = poly.names.foldRight(subst) { a, b -> b - a }
    TLCPolytype(poly.names, apply(substP, poly.type))
}

fun apply(subst: Subst, type: TLCMonotype): TLCMonotype = Fix.recursive().run {
    TLCMonotypeFFunctor.cata(type) {
        when (val type = it.fix()) {
            is TLCMonotypeF.Constant -> Eval.now(TLCMonotypeF.constant(type.name))
            is TLCMonotypeF.Variable -> Eval.now(subst.getOrDefault(type.name, TLCMonotypeF.variable(type.name)))
            is TLCMonotypeF.Apply -> Eval.monad().binding {
                TLCMonotypeF.apply(type.function.bind(), type.arg.bind())
            }.fix()
        }
    }
}

fun apply(subst: Subst, env: Env) = env.map { apply(subst, it) }

fun TLCPolytype.freeTypeVariables() = type.freeTypeVariables() - names.toSet()

fun TLCMonotype.freeTypeVariables(): Set<String> = Fix.recursive().run {
    TLCMonotypeFFunctor.cata(this@freeTypeVariables) {
        when (val type = it.fix()) {
            is TLCMonotypeF.Constant -> Eval.now(emptySet())
            is TLCMonotypeF.Variable -> Eval.now(setOf(type.name))
            is TLCMonotypeF.Apply -> Eval.monad().binding {
                type.function.bind() + type.arg.bind()
            }.fix()
        }
    }
}