package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

sealed class Expr {
    data class Var(val name: String) : Expr() {
        override fun toString() = name
    }

    data class Apply(val expr1: Expr, val expr2: Expr) : Expr() {
        override fun toString() = "($expr1) $expr2"
    }

    data class Abstract(val name: String, val expr: Expr) : Expr() {
        override fun toString() = "\\$name -> $expr"
    }

    data class Boolean(val bool: kotlin.Boolean) : Expr() {
        override fun toString() = "$bool"
    }
}
