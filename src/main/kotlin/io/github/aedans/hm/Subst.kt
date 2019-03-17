package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.typeclasses.*

/**
 * A substitution environment.
 */
typealias Subst<T> = Map<String, Monotype<T>>

/**
 * The empty substitution.
 */
fun <T> emptySubst(): Subst<T> = emptyMap()

/**
 * Composes a variadic number of substitutions.
 */
fun <T> Birecursive<T>.compose(vararg subst: Subst<T>): Subst<T> =
        subst.reduce { a, b -> compose(a, b) }

/**
 * Composes two substitutions, preferring values in the second substitution.
 */
fun <T> Birecursive<T>.compose(a: Subst<T>, b: Subst<T>): Subst<T> =
        (a + b).mapValues { apply(a, it.value) }

/**
 * Applies a substitution to a polytype.
 */
fun <T> Birecursive<T>.apply(subst: Subst<T>, poly: Polytype<T>): Polytype<T> =
        Polytype(
                poly.names,
                apply(poly.names.foldRight(subst) { a, b -> b - a }, poly.type)
        )

/**
 * Applies a substitution to a monotype.
 */
fun <T> Birecursive<T>.apply(subst: Subst<T>, type: Monotype<T>): Monotype<T> =
        MonotypeFactory(this).run {
            MonotypeFFunctor.cata(type) {
                when (val type = it.fix()) {
                    is MonotypeF.Constant -> constant(type.name)
                    is MonotypeF.Variable -> variable(type.name).map { subst.getOrDefault(type.name, it) }
                    is MonotypeF.Apply -> Eval.monad().binding {
                        apply(type.function.bind(), type.arg.bind()).bind()
                    }.fix()
                }
            }
        }

/**
 * Applies a substition to an environment.
 */
fun <T> Birecursive<T>.apply(subst: Subst<T>, env: Env<T>): Env<T> = env.map { apply(subst, it) }

/**
 * The type variables that are free in a monotype.
 */
fun <T> Recursive<T>.freeTypeVariables(type: Monotype<T>): Set<String> =
        MonotypeFFunctor.cata(type) {
            when (val type = it.fix()) {
                is MonotypeF.Constant -> Eval.now(emptySet())
                is MonotypeF.Variable -> Eval.now(setOf(type.name))
                is MonotypeF.Apply -> Eval.monad().binding {
                    type.function.bind() + type.arg.bind()
                }.fix()
            }
        }