package io.github.aedans.hm

import arrow.core.*
import arrow.core.extensions.either.monad.monad
import com.github.h0tk3y.betterParse.parser.parseToEnd
import org.testng.annotations.Test

@Test
class HindleyMilnerTest {
    private fun assertType(expr: String, type: String) {
        val result = Either.monad<InferenceError>().binding {
            val (_, from) = Grammar.exprParser.parseToEnd(Grammar.tokenizer.tokenize(expr)).infer(Env.empty).bind()
            val to = Grammar.typeParser.parseToEnd(Grammar.tokenizer.tokenize(type))
            unify(from, to).bind()
        }.fix()
        result.fold({ assert(false) { it.message } }, { })
    }

    private inline fun <reified T> assertFailsWith(expr: String) {
        Grammar.exprParser.parseToEnd(Grammar.tokenizer.tokenize(expr)).infer(Env.empty)
                .fold({ assert(it is T) { "Expected ${T::class}, found ${it::class}" } }, { })
    }

    fun id() = assertFailsWith<IsNotDefined>("x")

    fun bool() = assertType("true", "Bool")

    fun cast1() = assertType("true :: Bool", "Bool")
    fun cast2() = assertFailsWith<UnableToUnify>("true :: a -> b")
    fun id4() = assertType("(\\x -> x) :: Bool -> Bool", "Bool -> Bool")
    fun id5() = assertFailsWith<UnableToUnify>("(\\x -> x) :: Bool -> Int")

    fun arrow1() = assertType("\\x -> true", "a -> Bool")
    fun arrow2() = assertType("\\x -> \\y -> true", "a -> b -> Bool")
    fun arrow3() = assertType("\\x -> x", "a -> a")

    fun apply1() = assertType("\\x -> \\y -> x y", "(a -> b) -> a -> b")
    fun apply2() = assertType("\\x -> \\y -> \\z -> x y z", "(a -> b) -> (b -> c) -> b -> c")
    fun apply3() = assertType("\\x -> x true", "(Bool -> a) -> a")
    fun apply4() = assertType("\\x -> (x :: Bool)", "Bool -> Bool")
    fun apply5() = assertType("(\\x -> x) true", "Bool")
    fun apply6() = assertFailsWith<UnableToUnify>("(\\x -> x :: Int) true")

    fun infinite() = assertFailsWith<InfiniteBind>("\\x -> x x")

    fun if1() = assertType("if true then true else true", "Bool")
    fun if2() = assertType("\\x -> if x then true else x", "Bool -> Bool")
    fun if3() = assertType("\\x -> if x then x else x", "Bool -> Bool")
    fun if4() = assertType("\\x -> \\y -> if x then y else x", "Bool -> Bool -> Bool")
    fun if5() = assertType("\\x -> \\y -> \\z -> if x then y else z", "Bool -> a -> a -> a")
    fun if6() = assertFailsWith<UnableToUnify>("\\x -> if x then x :: String else x")
    fun if7() = assertFailsWith<UnableToUnify>("\\x -> if x then x else (x true)")
}
