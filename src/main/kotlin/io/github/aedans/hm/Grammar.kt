package io.github.aedans.hm

import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.use
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * Created by Aedan Smith.
 */

abstract class AbstractGrammar<out T> : Grammar<T>() {
    val ws by token("\\s+", ignore = true)
    val comment by token("\\/\\/.+", ignore = true)
    val oParen by token("\\(")
    val cParen by token("\\)")
    val oBrace by token("\\[")
    val cBrace by token("\\]")
    val oBracket by token("\\{")
    val cBracket by token("\\}")
    val backslash by token("\\\\")
    val dot by token("\\.")
    val eq by token("\\=")
    val arrow by token("\\-\\>")
    val extends by token("\\:\\:")
    val def by token("def")
    val `if` by token("if")
    val then by token("then")
    val `else` by token("else")
    val `true` by token("true")
    val `false` by token("false")
    val identifier by token("[a-zA-Z][_a-zA-Z0-9]*")
}

object ExprGrammar : AbstractGrammar<Expr>() {
    override val rootParser = parser { exprParser }

    val exprParser: Parser<Expr> = parser { applyExprParser }

    val applyExprParser: Parser<Expr> = parser { atomicExprParser } * parser(this::applyExprParser) use {
        Expr.Apply(t1, t2)
    } or parser { abstractExprParser }

    val abstractExprParser: Parser<Expr> = -backslash * identifier * -arrow * parser { exprParser } use {
        Expr.Abstract(t1.text, t2)
    } or parser { atomicExprParser }

    val atomicExprParser: Parser<Expr> = parser { parenthesizedExprParser } or
            parser { boolExprParser } or
            parser { varExprParser }

    val varExprParser: Parser<Expr.Var> = identifier use { Expr.Var(text) }

    val boolExprParser: Parser<Expr.Boolean> = (`true` or `false`) use { Expr.Boolean(text.toBoolean()) }

    val parenthesizedExprParser: Parser<Expr> = -oParen * parser { exprParser } * -cParen
}
