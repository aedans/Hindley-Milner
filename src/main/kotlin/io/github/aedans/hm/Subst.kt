package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.data.Fix
import arrow.recursion.extensions.fix.recursive.recursive

/**
 * Created by Aedan Smith.
 */

typealias Subst = Map<String, Monotype>

val emptySubst: Subst = emptyMap()

infix fun Subst.compose(subst: Subst) = (subst + this).mapValues { apply(this, it.value) }

fun apply(subst: Subst, poly: Polytype) = run {
    val substP = poly.names.foldRight(subst) { a, b -> b - a }
    Polytype(poly.names, apply(substP, poly.type))
}

fun apply(subst: Subst, type: Monotype): Monotype = Fix.recursive().run {
    MonotypeFFunctor.cata(type) {
        when (val type = it.fix()) {
            is MonotypeF.Constant -> Eval.now(MonotypeF.constant(type.name))
            is MonotypeF.Variable -> Eval.now(subst.getOrDefault(type.name, MonotypeF.variable(type.name)))
            is MonotypeF.Apply -> Eval.monad().binding {
                MonotypeF.apply(type.function.bind(), type.arg.bind())
            }.fix()
        }
    }
}

fun apply(subst: Subst, env: Env) = env.map { apply(subst, it) }

fun Polytype.freeTypeVariables() = type.freeTypeVariables() - names.toSet()

fun Monotype.freeTypeVariables(): Set<String> = Fix.recursive().run {
    MonotypeFFunctor.cata(this@freeTypeVariables) {
        when (val type = it.fix()) {
            is MonotypeF.Constant -> Eval.now(emptySet())
            is MonotypeF.Variable -> Eval.now(setOf(type.name))
            is MonotypeF.Apply -> Eval.monad().binding {
                type.function.bind() + type.arg.bind()
            }.fix()
        }
    }
}