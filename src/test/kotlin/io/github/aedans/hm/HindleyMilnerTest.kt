package io.github.aedans.hm

import com.github.h0tk3y.betterParse.parser.parseToEnd
import org.testng.Assert
import org.testng.annotations.Test

@Test
class HindleyMilnerTest {
    private fun assertType(expr: String, type: String) {
        unify(
                Grammar.exprParser.parseToEnd(Grammar.tokenizer.tokenize(expr)).infer(Env.empty).second,
                Grammar.typeParser.parseToEnd(Grammar.tokenizer.tokenize(type))
        )
    }

    private inline fun <reified T : Throwable> assertFailsWith(expr: String) {
        Assert.expectThrows(T::class.java) {
            Grammar.exprParser.parseToEnd(Grammar.tokenizer.tokenize(expr)).infer(Env.empty)
        }
    }

    fun id() = assertFailsWith<NoSuchElementException>("x")

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
