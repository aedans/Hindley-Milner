package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Expr.infer(env: Env): Pair<Subst, Type> = when (this) {
    is Expr.Bool -> emptySubst to Type.bool
    is Expr.Var -> env.get(name)?.let { emptySubst to it.instantiate() }
            ?: throw NoSuchElementException(name)
    is Expr.Cast -> {
        val (s1, exprType) = expr.infer(env)
        val s2 = unify(exprType, type)
        s2 compose s1 to apply(s2, exprType)
    }
    is Expr.Apply -> {
        val typeVariable = Type.Var(fresh())
        val (s1, expr1Type) = expr1.infer(env)
        val (s2, expr2Type) = expr2.infer(apply(s1, env))
        val s3 = unify(apply(s2, expr1Type), Type.Arrow(expr2Type, typeVariable).type)
        (s3 compose s2 compose s1) to apply(s3, typeVariable)
    }
    is Expr.Abstract -> {
        val arg = Type.Var(fresh())
        val envP = env.set(name, arg.scheme)
        val (s1, exprType) = expr.infer(envP)
        s1 to Type.Arrow(apply(s1, arg), exprType).type
    }
    is Expr.If -> {
        val (s1, conditionType) = condition.infer(env)
        val s2 = unify(conditionType, Type.bool)
        val (s3, expr1Type) = expr1.infer(apply(s2, env))
        val (s4, expr2Type) = expr2.infer(apply(s2, env))
        val s5 = unify(expr1Type, expr2Type)
        val t1 = apply(s5, expr1Type)
        val t2 = apply(s5, expr2Type)
        if (t1 != t2)
            throw Exception()
        (s5 compose s4 compose s3 compose s2 compose s1) to t1
    }
}
