package io.github.aedans.hm

import arrow.Kind
import arrow.core.*
import arrow.core.extensions.either.monad.monad
import arrow.recursion.typeclasses.*

/**
 * Unifies two types [a] and [b], or returns a [InferenceError] if the unification is not possible.
 */
fun <T> Birecursive<T>.unify(a: Monotype<T>, b: Monotype<T>): Either<InferenceError, Subst<T>> = run {
    val t1 = MonotypeFFunctor.projectT(a).fix()
    val t2 = MonotypeFFunctor.projectT(b).fix()
    when {
        t1 is MonotypeF.Apply && t2 is MonotypeF.Apply -> Either.monad<InferenceError>().binding {
            val s1 = unify(t1.function, t2.function).bind()
            val s2 = unify(apply(s1, t1.arg), apply(s1, t2.arg)).bind()
            compose(s2, s1)
        }.fix()
        t1 is MonotypeF.Variable -> bind(t1, b)
        t2 is MonotypeF.Variable -> bind(t2, a)
        t1 is MonotypeF.Constant && t2 is MonotypeF.Constant && t1 == t2 -> Right(emptySubst())
        else -> MonotypeShow(this).run { Left(UnableToUnify(a.show(), b.show())) }
    }
}

/**
 * Binds [variable] in [type], or returns an [InferenceError] if the binding would create an infinite type.
 */
fun <T> Recursive<T>.bind(variable: MonotypeF.Variable, type: Monotype<T>): Either<InferenceError, Subst<T>> = when {
    variable == MonotypeFFunctor.projectT(type) -> Right(emptySubst())
    occursIn(variable, type) -> MonotypeShow(this).run { Left(InfiniteBind(variable.name, type.show())) }
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
 * Error class for when when two types cannot be unified.
 */
class UnableToUnify(a: String, b: String) : InferenceError("Unable to unify $a and $b")

/**
 * Error class for when binding a variable would create an infinite type.
 */
class InfiniteBind(variable: String, type: String) : InferenceError("Infinite bind $variable to $type")