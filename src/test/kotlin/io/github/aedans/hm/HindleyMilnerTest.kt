package io.github.aedans.hm

import arrow.core.flatMap
import arrow.recursion.data.*
import arrow.recursion.extensions.mu.birecursive.birecursive
import arrow.recursion.typeclasses.Birecursive
import com.github.h0tk3y.betterParse.parser.parseToEnd
import org.testng.annotations.Test

@Test
class HindleyMilnerTest : Birecursive<ForMu> by Mu.birecursive() {
    val grammar = Grammar(MonotypeFactory(this), ExprFactory(this))

    private fun parseExpr(string: String) = grammar.exprParser.parseToEnd(grammar.tokenizer.tokenize(string))
    private fun parseType(string: String) = grammar.typeParser.parseToEnd(grammar.tokenizer.tokenize(string))

    private fun assertType(expr: String, type: String) {
        val result =
                infer(parseExpr(expr), Env.empty).flatMap { (_, from) ->
                    val to = parseType(type)
                    unify(from, to)
                }

        result.fold({ assert(false) { it.message } }, { })
    }

    private inline fun <reified T> assertFailsWith(expr: String) {
        infer(parseExpr(expr), Env.empty)
                .fold({ assert(it is T) { "Expected ${T::class}, found ${it::class}" } }, { })
    }

    fun id() = assertFailsWith<IsNotDefined>("x")

    fun bool() = assertType("true", "Bool")

    fun arrow1() = assertType("\\x -> true", "a -> Bool")
    fun arrow2() = assertType("\\x -> \\y -> true", "a -> b -> Bool")
    fun arrow3() = assertType("\\x -> x", "a -> a")

    fun apply1() = assertType("\\x -> \\y -> x y", "(a -> b) -> a -> b")
    fun apply2() = assertType("\\x -> \\y -> \\z -> x y z", "(a -> b) -> (b -> c) -> b -> c")
    fun apply3() = assertType("\\x -> x true", "(Bool -> a) -> a")
    fun apply4() = assertType("(\\x -> x) true", "Bool")

    fun infinite() = assertFailsWith<InfiniteBind>("\\x -> x x")

    fun if1() = assertType("if true then true else true", "Bool")
    fun if2() = assertType("\\x -> if x then true else x", "Bool -> Bool")
    fun if3() = assertType("\\x -> if x then x else x", "Bool -> Bool")
    fun if4() = assertType("\\x -> \\y -> if x then y else x", "Bool -> Bool -> Bool")
    fun if5() = assertType("\\x -> \\y -> \\z -> if x then y else z", "Bool -> a -> a -> a")
    fun if6() = assertFailsWith<UnableToUnify>("\\x -> if x then x else (x true)")
}
