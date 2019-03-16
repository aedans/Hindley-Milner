package io.github.aedans.hm

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * Created by Aedan Smith.
 */

object Grammar : com.github.h0tk3y.betterParse.grammar.Grammar<Expr>() {
    val ws by token("\\s+", ignore = true)
    val comment by token("\\/\\/.+", ignore = true)
    val oParen by token("\\(")
    val cParen by token("\\)")
    val backslash by token("\\\\")
    val eq by token("\\=")
    val arrow by token("\\-\\>")
    val extends by token("\\:\\:")
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

    val exprParser: Parser<Expr> = parser { abstractExprParser } or
            parser { ifExprParser } or
            parser { castExprParser }

    val abstractExprParser: Parser<Expr> = -backslash * identifier * -arrow * parser { exprParser } use {
        ExprF.abstract(t1.text, t2)
    }

    val ifExprParser: Parser<Expr> = -`if` * parser { exprParser } *
            -`then` * parser { exprParser } *
            -`else` * parser { exprParser } use {
        ExprF.cond(t1, t2, t3)
    }

    val castExprParser: Parser<Expr> = parser { applyExprParser } * -extends * parser { typeParser } use {
        ExprF.cast(t1, t2)
    } or parser { applyExprParser }

    val applyExprParser: Parser<Expr> = parser { atomicExprParser } * parser(this::applyExprParser) use {
        ExprF.apply(t1, t2)
    } or parser { atomicExprParser }

    val atomicExprParser: Parser<Expr> = parser { parenthesizedExprParser } or
            parser { boolExprParser } or
            parser { varExprParser }

    val varExprParser: Parser<Expr> = identifier use { ExprF.variable(text) }

    val boolExprParser: Parser<Expr> = (`true` or `false`) use { ExprF.bool(text.toBoolean()) }

    val parenthesizedExprParser: Parser<Expr> = -oParen * parser { exprParser } * -cParen

    // Types

    val typeParser: Parser<Monotype> = parser { functionTypeParser }

    val functionTypeParser: Parser<Monotype> = parser { applyTypeParser } * -arrow * parser(this::functionTypeParser) use {
        MonotypeF.arrow(t1, t2)
    } or parser { applyTypeParser }

    val applyTypeParser: Parser<Monotype> = parser { atomicTypeParser } * parser(this::applyTypeParser) use {
        MonotypeF.apply(t1, t2)
    } or parser { atomicTypeParser }

    val atomicTypeParser: Parser<Monotype> = parser { varTypeParser } or
            parser { constTypeParser } or
            parser { parenthesizedTypeParser }

    val parenthesizedTypeParser: Parser<Monotype> = -oParen * parser { typeParser } * -cParen

    val constTypeParser: Parser<Monotype> = constIdentifier use { MonotypeF.constant(text) }

    val varTypeParser: Parser<Monotype> = varIdentifier use { MonotypeF.variable(text) }
}