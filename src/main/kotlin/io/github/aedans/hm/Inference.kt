package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.either.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.extensions.fix.recursive.recursive

private operator fun <A, B> Eval<(A) -> B>.invoke(a: A): B = value()(a)

/**
 * Infers a type for an [Expr].
 */
fun Expr.infer(env: Env): Either<InferenceError, Pair<Subst, Monotype>> = Fix.recursive().run {
    ExprFFunctor.cata(this@infer, Algebra<ForExprF, Eval<(Env) -> Either<InferenceError, Pair<Subst, Monotype>>>> {
        fun impl(env: Env): Either<InferenceError, Pair<Subst, Monotype>> = when (val expr = it.fix()) {
            is ExprF.Bool -> Right(emptySubst to MonotypeF.bool)
            is ExprF.Variable -> {
                val type = env.get(expr.name)
                if (type == null) Left(IsNotDefined(expr.name))
                else Right(emptySubst to type.instantiate())
            }
            is ExprF.Apply -> Either.monad<InferenceError>().binding {
                val typeVariable = MonotypeF.variable(fresh())
                val (s1, expr1Type) = expr.function(env).bind()
                val (s2, expr2Type) = expr.arg(apply(s1, env)).bind()
                val s3 = unify(apply(s2, expr1Type), MonotypeF.arrow(expr2Type, typeVariable)).bind()
                (s3 compose s2 compose s1) to apply(s3, typeVariable)
            }.fix()
            is ExprF.Abstract -> Either.monad<InferenceError>().binding {
                val arg = MonotypeF.variable(fresh())
                val envP = env.set(expr.arg, arg.scheme)
                val (s1, exprType) = expr.body(envP).bind()
                s1 to MonotypeF.arrow(apply(s1, arg), exprType)
            }.fix()
            is ExprF.Cond -> Either.monad<InferenceError>().binding {
                val (s1, conditionType) = expr.condition(env).bind()
                val s2 = unify(conditionType, MonotypeF.bool).bind()
                val (s3, expr1Type) = expr.success(apply(s2, env)).bind()
                val (s4, expr2Type) = expr.failure(apply(s2, env)).bind()
                val s5 = unify(expr1Type, expr2Type).bind()
                val t1 = apply(s5, expr1Type)
                val t2 = apply(s5, expr2Type)
                if (t1 != t2)
                    throw Exception()
                (s5 compose s4 compose s3 compose s2 compose s1) to t1
            }.fix()
            is ExprF.Cast -> Either.monad<InferenceError>().binding {
                val (s1, exprType) = expr.expr(env).bind()
                val s2 = unify(exprType, expr.type).bind()
                s2 compose s1 to apply(s2, exprType)
            }.fix()
        }

        Eval.now { env: Env -> impl(env) }
    })
}(env)

/**
 * Abstract class of errors during inference.
 */
abstract class InferenceError(val message: String)

/**
 * Error when something is not defined.
 */
class IsNotDefined(name: String) : InferenceError("$name is not defined")