package io.github.aedans.hm

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * Created by Aedan Smith.
 */

object Grammar : com.github.h0tk3y.betterParse.grammar.Grammar<TLCExpr>() {
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

    val exprParser: Parser<TLCExpr> = parser { abstractExprParser } or
            parser { ifExprParser } or
            parser { castExprParser }

    val abstractExprParser: Parser<TLCExpr> = -backslash * identifier * -arrow * parser { exprParser } use {
        TLCExprF.abstract(t1.text, t2)
    }

    val ifExprParser: Parser<TLCExpr> = -`if` * parser { exprParser } *
            -`then` * parser { exprParser } *
            -`else` * parser { exprParser } use {
        TLCExprF.cond(t1, t2, t3)
    }

    val castExprParser: Parser<TLCExpr> = parser { applyExprParser } * -extends * parser { typeParser } use {
        TLCExprF.cast(t1, t2)
    } or parser { applyExprParser }

    val applyExprParser: Parser<TLCExpr> = parser { atomicExprParser } * parser(this::applyExprParser) use {
        TLCExprF.apply(t1, t2)
    } or parser { atomicExprParser }

    val atomicExprParser: Parser<TLCExpr> = parser { parenthesizedExprParser } or
            parser { boolExprParser } or
            parser { varExprParser }

    val varExprParser: Parser<TLCExpr> = identifier use { TLCExprF.variable(text) }

    val boolExprParser: Parser<TLCExpr> = (`true` or `false`) use { TLCExprF.bool(text.toBoolean()) }

    val parenthesizedExprParser: Parser<TLCExpr> = -oParen * parser { exprParser } * -cParen

    // Types

    val typeParser: Parser<TLCMonotype> = parser { functionTypeParser }

    val functionTypeParser: Parser<TLCMonotype> = parser { applyTypeParser } * -arrow * parser(this::functionTypeParser) use {
        TLCMonotypeF.arrow(t1, t2)
    } or parser { applyTypeParser }

    val applyTypeParser: Parser<TLCMonotype> = parser { atomicTypeParser } * parser(this::applyTypeParser) use {
        TLCMonotypeF.apply(t1, t2)
    } or parser { atomicTypeParser }

    val atomicTypeParser: Parser<TLCMonotype> = parser { varTypeParser } or
            parser { constTypeParser } or
            parser { parenthesizedTypeParser }

    val parenthesizedTypeParser: Parser<TLCMonotype> = -oParen * parser { typeParser } * -cParen

    val constTypeParser: Parser<TLCMonotype> = constIdentifier use { TLCMonotypeF.constant(text) }

    val varTypeParser: Parser<TLCMonotype> = varIdentifier use { TLCMonotypeF.variable(text) }
}