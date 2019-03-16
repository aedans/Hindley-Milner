package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.extensions.fix.recursive.recursive

/**
 * Infers a type for a [Expr].
 */
fun Expr.infer(env: Env): Pair<Subst, Monotype> = Fix.recursive().run {
    ExprFFunctor.cata(this@infer, Algebra<ForExprF, Eval<(Env) -> Eval<Pair<Subst, Monotype>>>> {
        fun impl(env: Env) = when (val expr = it.fix()) {
            is ExprF.Bool -> Eval.now(emptySubst to MonotypeF.bool)
            is ExprF.Variable -> env.get(expr.name)?.let { Eval.now(emptySubst to it.instantiate()) }
                    ?: throw NoSuchElementException(expr.name)
            is ExprF.Apply -> Eval.monad().binding {
                val typeVariable = MonotypeF.variable(fresh())
                val (s1, expr1Type) = expr.function.bind()(env).bind()
                val (s2, expr2Type) = expr.arg.bind()(apply(s1, env)).bind()
                val s3 = unify(apply(s2, expr1Type), MonotypeF.arrow(expr2Type, typeVariable))
                (s3 compose s2 compose s1) to apply(s3, typeVariable)
            }.fix()
            is ExprF.Abstract -> Eval.monad().binding {
                val arg = MonotypeF.variable(fresh())
                val envP = env.set(expr.arg, arg.scheme)
                val (s1, exprType) = expr.body.bind()(envP).bind()
                s1 to MonotypeF.arrow(apply(s1, arg), exprType)
            }.fix()
            is ExprF.Cond -> Eval.monad().binding {
                val (s1, conditionType) = expr.condition.bind()(env).bind()
                val s2 = unify(conditionType, MonotypeF.bool)
                val (s3, expr1Type) = expr.success.bind()(apply(s2, env)).bind()
                val (s4, expr2Type) = expr.failure.bind()(apply(s2, env)).bind()
                val s5 = unify(expr1Type, expr2Type)
                val t1 = apply(s5, expr1Type)
                val t2 = apply(s5, expr2Type)
                if (t1 != t2)
                    throw Exception()
                (s5 compose s4 compose s3 compose s2 compose s1) to t1
            }.fix()
            is ExprF.Cast -> Eval.monad().binding {
                val (s1, exprType) = expr.expr.bind()(env).bind()
                val s2 = unify(exprType, expr.type)
                s2 compose s1 to apply(s2, exprType)
            }.fix()
        }

        Eval.now { env: Env -> impl(env) }
    })
}(env).value()