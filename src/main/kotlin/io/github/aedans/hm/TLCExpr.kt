package io.github.aedans.hm

/**
 * An algebraic data type representing a typed lambda calculus expression.
 */
sealed class TLCExpr {
    /**
     * A data class representing a variable expression.
     *
     * @param name The name of the variable.
     */
    data class Var(val name: String) : TLCExpr() {
        override fun toString() = name
    }

    /**
     * A data class representing a boolean literal.
     *
     * @param bool The value of the boolean.
     */
    data class Bool(val bool: Boolean) : TLCExpr() {
        override fun toString() = "$bool"
    }

    /**
     * A data class representing expression application.
     *
     * @param function The function being applied.
     * @param arg      The argument of the function.
     */
    data class Apply(val function: TLCExpr, val arg: TLCExpr) : TLCExpr() {
        override fun toString() = "($function) $arg"
    }

    /**
     * A data class representing expression abstraction.
     *
     * @param arg  The argument of the function.
     * @param body The body of the expression.
     */
    data class Abstract(val arg: String, val body: TLCExpr) : TLCExpr() {
        override fun toString() = "\\$arg -> $body"
    }

    /**
     * A data class representing an if expression.
     *
     * @param condition The condition of the expression.
     * @param success   The result of the expression if the condition is true.
     * @param failure   The result of the expression if the condition is false.
     */
    data class If(val condition: TLCExpr, val success: TLCExpr, val failure: TLCExpr) : TLCExpr() {
        override fun toString() = "if $condition then $success else $failure"
    }

    /**
     * A data class representing expression casting.
     *
     * @param expr The expression to cast.
     * @param type The type to cast to.
     */
    data class Cast(val expr: TLCExpr, val type: TLCType.Mono) : TLCExpr() {
        override fun toString() = "($expr) :: $type"
    }
}
