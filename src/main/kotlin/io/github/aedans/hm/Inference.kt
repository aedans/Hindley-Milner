package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.either.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.typeclasses.Birecursive

/**
 * The result of inferring the type of an expression.
 */
typealias InferResult<T> = Either<InferenceError, Pair<Subst<T>, Monotype<T>>>

/**
 * Infers a type for an expression.
 */
fun <T> Birecursive<T>.infer(expr: Expr<T>, env: Env<T>): InferResult<T> = MonotypeFactory(this).run {
    fun impl(it: ExprF<Eval<(Env<T>) -> InferResult<T>>>, env: Env<T>): InferResult<T> = when (val expr = it.fix()) {
        is ExprF.Bool -> Right(emptySubst<T>() to bool.value())
        is ExprF.Variable -> {
            val type = env.get(expr.name)
            if (type == null) Left(IsNotDefined(expr.name))
            else Right(emptySubst<T>() to instantiate(type))
        }
        is ExprF.Apply -> Either.monad<InferenceError>().binding {
            val typeVariable = variable(fresh()).value()
            val (s1, expr1Type) = expr.function(env).bind()
            val (s2, expr2Type) = expr.arg(apply(s1, env)).bind()
            val s3 = unify(apply(s2, expr1Type), arrow(expr2Type, typeVariable).value()).bind()
            compose(s3, s2, s1) to apply(s3, typeVariable)
        }.fix()
        is ExprF.Abstract -> Either.monad<InferenceError>().binding {
            val arg = variable(fresh()).value()
            val envP = env.set(expr.arg, poly(arg))
            val (s1, exprType) = expr.body(envP).bind()
            s1 to arrow(apply(s1, arg), exprType).value()
        }.fix()
        is ExprF.Cond -> Either.monad<InferenceError>().binding {
            val (s1, conditionType) = expr.condition(env).bind()
            val s2 = unify(conditionType, bool.value()).bind()
            val (s3, expr1Type) = expr.success(apply(s2, env)).bind()
            val (s4, expr2Type) = expr.failure(apply(s2, env)).bind()
            val s5 = unify(expr1Type, expr2Type).bind()
            val t1 = apply(s5, expr1Type)
            compose(s5, s4, s3, s2, s1) to t1
        }.fix()
    }

    ExprFFunctor.cata(expr, Algebra<ForExprF, Eval<(Env<T>) -> InferResult<T>>> {
        Eval.now { env: Env<T> -> impl(it.fix(), env) }
    })
}(env)

private operator fun <A, B> Eval<(A) -> B>.invoke(a: A): B = value()(a)

/**
 * Abstract class of errors during inference.
 */
abstract class InferenceError(val message: String)

/**
 * Error when something is not defined.
 */
class IsNotDefined(name: String) : InferenceError("$name is not defined")