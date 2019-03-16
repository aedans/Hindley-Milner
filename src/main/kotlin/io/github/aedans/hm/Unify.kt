package io.github.aedans.hm

import arrow.Kind
import arrow.core.*
import arrow.core.extensions.either.monad.monad
import arrow.recursion.data.*
import arrow.recursion.extensions.fix.recursive.recursive
import arrow.recursion.typeclasses.Recursive

/**
 * Unifies two types [a] and [b], or returns a [InferenceError] if the unification is not possible.
 */
fun unify(a: Monotype, b: Monotype): Either<InferenceError, Subst> = run {
    val a = a.unfix.fix()
    val b = b.unfix.fix()
    when {
        a is MonotypeF.Apply && b is MonotypeF.Apply -> Either.monad<InferenceError>().binding {
            val s1 = unify(a.function.value().fix(), b.function.value().fix()).bind()
            val s2 = unify(apply(s1, a.arg.value().fix()), apply(s1, b.arg.value().fix())).bind()
            s2 compose s1
        }.fix()
        a is MonotypeF.Variable -> bind(a, Fix(b))
        b is MonotypeF.Variable -> bind(b, Fix(a))
        a is MonotypeF.Constant && b is MonotypeF.Constant && a == b -> Right(emptySubst)
        else -> Left(UnableToUnify(Fix(a), Fix(b)))
    }
}

/**
 * Binds [variable] in [type], or returns an [InferenceError] if the binding would create an infinite type.
 */
fun bind(variable: MonotypeF.Variable, type: Monotype): Either<InferenceError, Subst> = when {
    variable == type.unfix -> Right(emptySubst)
    Fix.recursive().occursIn(variable, type) -> Left(InfiniteBind(variable, type))
    else -> Right(mapOf(variable.name to type))
}

/**
 * Checks if [variable] occurs in [type].
 */
fun <T> Recursive<T>.occursIn(variable: MonotypeF.Variable, type: Kind<T, ForMonotypeF>): Boolean =
    MonotypeFFunctor.cata(type) {
        when (val type = it.fix()) {
            is MonotypeF.Constant -> Eval.now(false)
            is MonotypeF.Variable -> Eval.later { type == variable }
            is MonotypeF.Apply -> type.function.flatMap { a -> type.arg.map { b -> a || b } }
        }
    }

/**
 * Error class for when when two types [a] and [b] cannot be unified.
 */
class UnableToUnify(a: Monotype, b: Monotype) : InferenceError("Unable to unify $a and $b")

/**
 * Error class for when binding [variable] in [type] would create an infinite type.
 */
class InfiniteBind(variable: MonotypeF.Variable, type: Monotype) : InferenceError("Infinite bind $variable to $type")