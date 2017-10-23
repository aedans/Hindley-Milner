package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Expr.infer(env: Env): Pair<Subst, Type> = when (this) {
    is Expr.Boolean -> emptySubst to Type.Const("Bool")
    is Expr.Var -> env.get(name)!!.let { emptySubst to it.instantiate() }
    is Expr.Cast -> {
        val (exprSubst, exprType) = expr.infer(env)
        exprSubst to apply(unify(exprType, type), exprType)
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
}
