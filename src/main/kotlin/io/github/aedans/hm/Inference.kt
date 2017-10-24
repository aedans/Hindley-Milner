package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Expr.infer(env: Env): Pair<Subst, Type> = when (this) {
    is Expr.Bool -> emptySubst to Type.bool
    is Expr.Var -> env.get(name)!!.let { emptySubst to it.instantiate() }
    is Expr.Cast -> {
        val (exprSubst, exprType) = expr.infer(env)
        val substP = unify(exprType, type)
        substP compose exprSubst to apply(substP, exprType)
    }
    is Expr.Apply -> {
        val typeVariable = Type.Var(fresh())
        val (expr1Subst, expr1Type) = expr1.infer(env)
        val (expr2Subst, expr2Type) = expr2.infer(apply(expr1Subst, env))
        val substP = unify(apply(expr2Subst, expr1Type), Type.Arrow(expr2Type, typeVariable).type)
        (substP compose expr2Subst compose expr1Subst) to apply(substP, typeVariable)
    }
    is Expr.Abstract -> {
        val arg = Type.Var(fresh())
        val envP = env.set(name, arg.scheme)
        val (exprSubst, exprType) = expr.infer(envP)
        exprSubst to Type.Arrow(apply(exprSubst, arg), exprType).type
    }
    is Expr.If -> {
        val (conditionSubst, conditionType) = condition.infer(env)
        val conditionSubst2 = unify(conditionType, Type.bool) compose conditionSubst
        val (expr1Subst, expr1Type) = expr1.infer(apply(conditionSubst2, env))
        val (expr2Subst, expr2Type) = expr2.infer(apply(expr1Subst, env))
        val substP = unify(expr1Type, expr2Type)
        val t1 = apply(substP, expr1Type)
        val t2 = apply(substP, expr2Type)
        if (t1 != t2)
            throw Exception()
        (substP compose expr2Subst compose expr1Subst compose conditionSubst2 compose conditionSubst) to t1
    }
}
