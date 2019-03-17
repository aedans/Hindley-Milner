package io.github.aedans.hm

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * The grammar for typed lambda calculus
 */
class Grammar<T>(monotypeFactory: MonotypeFactory<T>, exprFactory: ExprFactory<T>) : com.github.h0tk3y.betterParse.grammar.Grammar<Expr<T>>() {
    val ws by token("\\s+", ignore = true)
    val comment by token("\\/\\/.+", ignore = true)
    val oParen by token("\\(")
    val cParen by token("\\)")
    val backslash by token("\\\\")
    val eq by token("\\=")
    val arrow by token("\\-\\>")
    val `if` by token("if")
    val then by token("then")
    val `else` by token("else")
    val `true` by token("true")
    val `false` by token("false")
    val constIdentifier by token("[A-Z][_a-zA-Z0-9]*")
    val varIdentifier by token("[a-z][_a-zA-Z0-9]*")

    val identifier = constIdentifier or varIdentifier

    override val rootParser = parser { exprParser }

    // Exprs

    val exprParser: Parser<Expr<T>> = parser { abstractExprParser } or
            parser { ifExprParser } or
            parser { applyExprParser }

    val abstractExprParser: Parser<Expr<T>> = -backslash * identifier * -arrow * parser { exprParser } use {
        exprFactory.abstract(t1.text, t2).value()
    }

    val ifExprParser: Parser<Expr<T>> = -`if` * parser { exprParser } *
            -`then` * parser { exprParser } *
            -`else` * parser { exprParser } use {
        exprFactory.cond(t1, t2, t3).value()
    }

    val applyExprParser: Parser<Expr<T>> = parser { atomicExprParser } * parser(this::applyExprParser) use {
        exprFactory.apply(t1, t2).value()
    } or parser { atomicExprParser }

    val atomicExprParser: Parser<Expr<T>> = parser { parenthesizedExprParser } or
            parser { boolExprParser } or
            parser { varExprParser }

    val varExprParser: Parser<Expr<T>> = identifier use { exprFactory.variable(text).value() }

    val boolExprParser: Parser<Expr<T>> = (`true` or `false`) use { exprFactory.bool(text.toBoolean()).value() }

    val parenthesizedExprParser: Parser<Expr<T>> = -oParen * parser { exprParser } * -cParen

    // Types

    val typeParser: Parser<Monotype<T>> = parser { functionTypeParser }

    val functionTypeParser: Parser<Monotype<T>> = parser { applyTypeParser } * -arrow * parser(this::functionTypeParser) use {
        monotypeFactory.arrow(t1, t2).value()
    } or parser { applyTypeParser }

    val applyTypeParser: Parser<Monotype<T>> = parser { atomicTypeParser } * parser(this::applyTypeParser) use {
        monotypeFactory.apply(t1, t2).value()
    } or parser { atomicTypeParser }

    val atomicTypeParser: Parser<Monotype<T>> = parser { varTypeParser } or
            parser { constTypeParser } or
            parser { parenthesizedTypeParser }

    val parenthesizedTypeParser: Parser<Monotype<T>> = -oParen * parser { typeParser } * -cParen

    val constTypeParser: Parser<Monotype<T>> = constIdentifier use { monotypeFactory.constant(text).value() }

    val varTypeParser: Parser<Monotype<T>> = varIdentifier use { monotypeFactory.variable(text).value() }
}