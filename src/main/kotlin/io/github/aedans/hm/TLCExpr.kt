package io.github.aedans.hm

import arrow.*
import arrow.core.Eval.Companion.now
import arrow.recursion.data.Fix
import arrow.typeclasses.Functor

/**
 * The fixed point of [TLCExprF].
 */
typealias TLCExpr = Fix<ForTLCExprF>

/**
 * An algebraic data type representing a typed lambda calculus expression.
 *
 * @param Self The fixed point of this type.
 */
@higherkind
sealed class TLCExprF<out Self> : TLCExprFOf<Self> {
    /**
     * A data class representing a variable expression.
     *
     * @param name The name of the variable.
     */
    data class Variable(val name: String) : TLCExprF<Nothing>() {
        override fun toString() = name
    }

    /**
     * A data class representing a boolean literal.
     *
     * @param bool The value of the boolean.
     */
    data class Bool(val bool: Boolean) : TLCExprF<Nothing>() {
        override fun toString() = "$bool"
    }

    /**
     * A data class representing expression application.
     *
     * @param function The function being applied.
     * @param arg      The argument of the function.
     */
    data class Apply<out Self>(val function: Self, val arg: Self) : TLCExprF<Self>() {
        override fun toString() = "($function) $arg"
    }

    /**
     * A data class representing expression abstraction.
     *
     * @param arg  The argument of the function.
     * @param body The body of the expression.
     */
    data class Abstract<out Self>(val arg: String, val body: Self) : TLCExprF<Self>() {
        override fun toString() = "\\$arg -> $body"
    }

    /**
     * A data class representing a conditional expression.
     *
     * @param condition The condition of the expression.
     * @param success   The result of the expression if the condition is true.
     * @param failure   The result of the expression if the condition is false.
     */
    data class Cond<out Self>(val condition: Self, val success: Self, val failure: Self) : TLCExprF<Self>() {
        override fun toString() = "if $condition then $success else $failure"
    }

    /**
     * A data class representing expression casting.
     *
     * @param expr The expression to cast.
     * @param type The type to cast to.
     */
    data class Cast<out Self>(val expr: Self, val type: TLCMonotype) : TLCExprF<Self>() {
        override fun toString() = "($expr) :: $type"
    }

    companion object {
        fun variable(name: String) = Fix(Variable(name))
        fun bool(bool: Boolean) = Fix(Bool(bool))
        fun apply(function: TLCExpr, arg: TLCExpr) = Fix(Apply(now(function), now(arg)))
        fun abstract(arg: String, body: TLCExpr) = Fix(Abstract(arg, now(body)))
        fun cond(condition: TLCExpr, success: TLCExpr, failure: TLCExpr) = Fix(Cond(now(condition), now(success), now(failure)))
        fun cast(expr: TLCExpr, type: TLCMonotype) = Fix(Cast(now(expr), type))
    }
}

/**
 * A functor instance for [TLCExprF].
 */
object TLCExprFFunctor : Functor<ForTLCExprF> {
    override fun <A, B> TLCExprFOf<A>.map(f: (A) -> B) = when (val expr = fix()) {
        is TLCExprF.Variable -> expr
        is TLCExprF.Bool -> expr
        is TLCExprF.Apply -> TLCExprF.Apply(f(expr.function), f(expr.arg))
        is TLCExprF.Abstract -> TLCExprF.Abstract(expr.arg, f(expr.body))
        is TLCExprF.Cond -> TLCExprF.Cond(f(expr.condition), f(expr.success), f(expr.failure))
        is TLCExprF.Cast -> TLCExprF.Cast(f(expr.expr), expr.type)
    }
}