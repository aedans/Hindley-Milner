package io.github.aedans.hm

import arrow.*
import arrow.core.*
import arrow.core.Eval.Companion.now
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.data.Fix
import arrow.recursion.extensions.fix.recursive.recursive
import arrow.recursion.typeclasses.Recursive
import arrow.typeclasses.Functor

/**
 * The fixed point of [ExprF].
 */
typealias Expr = Fix<ForExprF>

/**
 * An algebraic data type representing a typed lambda calculus expression.
 *
 * @param Self The fixed point of this type.
 */
@higherkind
sealed class ExprF<out Self> : ExprFOf<Self> {
    /**
     * A data class representing a variable expression.
     *
     * @param name The name of the variable.
     */
    data class Variable(val name: String) : ExprF<Nothing>() {
        override fun toString() = name
    }

    /**
     * A data class representing a boolean literal.
     *
     * @param bool The value of the boolean.
     */
    data class Bool(val bool: Boolean) : ExprF<Nothing>() {
        override fun toString() = "$bool"
    }

    /**
     * A data class representing expression application.
     *
     * @param function The function being applied.
     * @param arg      The argument of the function.
     */
    data class Apply<out Self>(val function: Self, val arg: Self) : ExprF<Self>() {
        override fun toString() = "($function) $arg"
    }

    /**
     * A data class representing expression abstraction.
     *
     * @param arg  The argument of the function.
     * @param body The body of the expression.
     */
    data class Abstract<out Self>(val arg: String, val body: Self) : ExprF<Self>() {
        override fun toString() = "\\$arg -> $body"
    }

    /**
     * A data class representing a conditional expression.
     *
     * @param condition The condition of the expression.
     * @param success   The result of the expression if the condition is true.
     * @param failure   The result of the expression if the condition is false.
     */
    data class Cond<out Self>(val condition: Self, val success: Self, val failure: Self) : ExprF<Self>() {
        override fun toString() = "if $condition then $success else $failure"
    }

    /**
     * A data class representing expression casting.
     *
     * @param expr The expression to cast.
     * @param type The type to cast to.
     */
    data class Cast<out Self>(val expr: Self, val type: Monotype) : ExprF<Self>() {
        override fun toString() = "($expr) :: $type"
    }

    companion object {
        fun variable(name: String) = Fix(Variable(name))
        fun bool(bool: Boolean) = Fix(Bool(bool))
        fun apply(function: Expr, arg: Expr) = Fix(Apply(now(function), now(arg)))
        fun abstract(arg: String, body: Expr) = Fix(Abstract(arg, now(body)))
        fun cond(condition: Expr, success: Expr, failure: Expr) = Fix(Cond(now(condition), now(success), now(failure)))
        fun cast(expr: Expr, type: Monotype) = Fix(Cast(now(expr), type))
    }
}

/**
 * A functor instance for [ExprF].
 */
object ExprFFunctor : Functor<ForExprF> {
    override fun <A, B> ExprFOf<A>.map(f: (A) -> B) = when (val expr = fix()) {
        is ExprF.Variable -> expr
        is ExprF.Bool -> expr
        is ExprF.Apply -> ExprF.Apply(f(expr.function), f(expr.arg))
        is ExprF.Abstract -> ExprF.Abstract(expr.arg, f(expr.body))
        is ExprF.Cond -> ExprF.Cond(f(expr.condition), f(expr.success), f(expr.failure))
        is ExprF.Cast -> ExprF.Cast(f(expr.expr), expr.type)
    }
}

fun toString(expr: Expr) = Fix.recursive().toString(expr)

fun <T> Recursive<T>.toString(expr: Kind<T, ForExprF>) =
        ExprFFunctor.cata<ForExprF, Pair<String, Boolean>>(expr) {
            when (val expr = it.fix()) {
                is ExprF.Variable -> Eval.now(expr.name to true)
                is ExprF.Bool -> Eval.now(expr.bool.toString() to true)
                is ExprF.Apply -> Eval.monad().binding {
                    val (function, atomic) = expr.function.bind()
                    val (arg, _) = expr.arg.bind()
                    (if (atomic) "$function $arg" else "($function) $arg") to false
                }.fix()
                is ExprF.Abstract -> Eval.monad().binding {
                    "\\${expr.arg} -> ${expr.body.bind().first}" to false
                }.fix()
                is ExprF.Cond -> Eval.monad().binding {
                    "if ${expr.condition.bind().first} " +
                            "then ${expr.success.bind().first} " +
                            "else ${expr.failure.bind().first}" to false
                }.fix()
                is ExprF.Cast -> Eval.monad().binding {
                    "${expr.expr.bind().first} :: ${toString(expr.type)}" to false
                }.fix()
            }
        }.first