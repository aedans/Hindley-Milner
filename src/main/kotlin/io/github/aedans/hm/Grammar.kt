package io.github.aedans.hm

import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.use
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

    val exprParser: Parser<Expr> = parser { applyExprParser }

    val applyExprParser: Parser<Expr> = parser { abstractExprParser } * parser(this::applyExprParser) use {
        Expr.Apply(t1, t2)
    } or parser { abstractExprParser }

    val abstractExprParser: Parser<Expr> = -backslash * identifier * -arrow * parser { exprParser } use {
        Expr.Abstract(t1.text, t2)
    } or parser { castExprParser }

    val castExprParser: Parser<Expr> = parser { atomicExprParser } * -extends * parser { typeParser } use {
        Expr.Cast(t1, t2)
    } or parser { atomicExprParser }

    val atomicExprParser: Parser<Expr> = parser { parenthesizedExprParser } or
            parser { boolExprParser } or
            parser { varExprParser }

    val varExprParser: Parser<Expr.Var> = identifier use { Expr.Var(text) }

    val boolExprParser: Parser<Expr.Boolean> = (`true` or `false`) use { Expr.Boolean(text.toBoolean()) }

    val parenthesizedExprParser: Parser<Expr> = -oParen * parser { exprParser } * -cParen

    val typeParser: Parser<Type> = parser { functionTypeParser }

    val functionTypeParser: Parser<Type> = parser { applyTypeParser } * -arrow * parser(this::functionTypeParser) use {
        Type.Arrow(t1, t2).type
    } or parser { applyTypeParser }

    val applyTypeParser: Parser<Type> = parser { atomicTypeParser } * parser(this::applyTypeParser) use {
        Type.Apply(t1, t2)
    } or parser { atomicTypeParser }

    val atomicTypeParser: Parser<Type> = parser { varTypeParser } or
            parser { constTypeParser } or
            parser { parenthesizedTypeParser }

    val parenthesizedTypeParser: Parser<Type> = -oParen * parser { typeParser } * -cParen

    val constTypeParser: Parser<Type.Const> = constIdentifier use { Type.Const(text) }

    val varTypeParser: Parser<Type.Var> = varIdentifier use { Type.Var(text) }
}
