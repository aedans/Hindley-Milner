package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Expr.infer(env: Env): Pair<Subst, Type> = when (this) {
    is Expr.Boolean -> emptySubst to Type.Const("Bool")
    is Expr.Var -> env.getEnv(name)
    is Expr.Apply -> {
        val typeVariable = Type.Var(fresh())
        val (expr1Subst, expr1Type) = expr1.infer(env)
        val (expr2Subst, expr2Type) = expr2.infer(apply(expr1Subst, env))
        val substP = unify(apply(expr2Subst, expr1Type), Type.Arrow(expr2Type, typeVariable).type)
        (substP compose expr2Subst compose expr1Subst) to apply(substP, typeVariable)
    }
    is Expr.Abstract -> {
        val typeVariable = Type.Var(fresh())
        val envP = env + mapOf(name to typeVariable.scheme)
        val (exprSubst, exprType) = expr.infer(envP)
        exprSubst to Type.Arrow(apply(exprSubst, typeVariable), exprType).type
    }
}

fun Env.getEnv(name: String) = this[name]!!.let { emptySubst to it.instantiate() }
