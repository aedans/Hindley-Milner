package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.extensions.fix.recursive.recursive

/**
 * The inference algorithm for a [TLCExpr].
 */
fun inferAlgebra(): Algebra<ForTLCExprF, Eval<(Env) -> Eval<Pair<Subst, TLCType.Mono>>>> = Algebra {
    fun impl(env: Env) = when (val expr = it.fix()) {
        is TLCExprF.Bool -> Eval.now(emptySubst to TLCType.Mono.bool)
        is TLCExprF.Variable -> env.get(expr.name)?.let { Eval.now(emptySubst to it.instantiate()) }
                ?: throw NoSuchElementException(expr.name)
        is TLCExprF.Apply -> Eval.monad().binding {
            val typeVariable = TLCType.Mono.Var(fresh())
            val (s1, expr1Type) = expr.function.bind()(env).bind()
            val (s2, expr2Type) = expr.arg.bind()(apply(s1, env)).bind()
            val s3 = unify(apply(s2, expr1Type), TLCType.Mono.Arrow(expr2Type, typeVariable).type)
            (s3 compose s2 compose s1) to apply(s3, typeVariable)
        }.fix()
        is TLCExprF.Abstract -> Eval.monad().binding {
            val arg = TLCType.Mono.Var(fresh())
            val envP = env.set(expr.arg, arg.scheme)
            val (s1, exprType) = expr.body.bind()(envP).bind()
            s1 to TLCType.Mono.Arrow(apply(s1, arg), exprType).type
        }.fix()
        is TLCExprF.Cond -> Eval.monad().binding {
            val (s1, conditionType) = expr.condition.bind()(env).bind()
            val s2 = unify(conditionType, TLCType.Mono.bool)
            val (s3, expr1Type) = expr.success.bind()(apply(s2, env)).bind()
            val (s4, expr2Type) = expr.failure.bind()(apply(s2, env)).bind()
            val s5 = unify(expr1Type, expr2Type)
            val t1 = apply(s5, expr1Type)
            val t2 = apply(s5, expr2Type)
            if (t1 != t2)
                throw Exception()
            (s5 compose s4 compose s3 compose s2 compose s1) to t1
        }.fix()
        is TLCExprF.Cast -> Eval.monad().binding {
            val (s1, exprType) = expr.expr.bind()(env).bind()
            val s2 = unify(exprType, expr.type)
            s2 compose s1 to apply(s2, exprType)
        }.fix()
    }

    Eval.now { env: Env -> impl(env) }
}

fun TLCExpr.infer(env: Env) = Fix.recursive().run {
    TLCExprFFunctor.cata(this@infer, inferAlgebra())
}(env).value()