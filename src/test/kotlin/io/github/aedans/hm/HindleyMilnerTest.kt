package io.github.aedans.hm

import com.github.h0tk3y.betterParse.parser.parseToEnd
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by Aedan Smith.
 */

@Test
class HindleyMilnerTest {
    private fun assert(expr: String, type: String) {
        unify(
                Grammar.exprParser.parseToEnd(Grammar.lexer.tokenize(expr)).infer(Env.empty).second,
                Grammar.typeParser.parseToEnd(Grammar.lexer.tokenize(type))
        )
    }

    private inline fun <reified T : Throwable> nassert(expr: String) {
        Assert.expectThrows(T::class.java) {
            Grammar.exprParser.parseToEnd(Grammar.lexer.tokenize(expr)).infer(Env.empty)
        }
    }

    fun id() = nassert<NoSuchElementException>("x")

    fun bool() = assert("true", "Bool")

    fun cast1() = assert("true :: Bool", "Bool")
    fun cast2() = nassert<UnableToUnify>("true :: a -> b")
    fun id4() = assert("(\\x -> x) :: Bool -> Bool", "Bool -> Bool")
    fun id5() = nassert<UnableToUnify>("(\\x -> x) :: Bool -> Int")

    fun arrow1() = assert("\\x -> true", "a -> Bool")
    fun arrow2() = assert("\\x -> \\y -> true", "a -> b -> Bool")
    fun arrow3() = assert("\\x -> x", "a -> a")

    fun apply1() = assert("\\x -> \\y -> x y", "(a -> b) -> a -> b")
    fun apply2() = assert("\\x -> \\y -> \\z -> x y z", "(a -> b) -> (b -> c) -> b -> c")
    fun apply3() = assert("\\x -> x true", "(Bool -> a) -> a")
    fun apply4() = assert("\\x -> (x :: Bool)", "Bool -> Bool")
    fun apply5() = assert("(\\x -> x) true", "Bool")
    fun apply6() = nassert<UnableToUnify>("(\\x -> x :: Int) true")

    fun infinite() = nassert<InfiniteBind>("\\x -> x x")

    fun if1() = assert("if true then true else true", "Bool")
    fun if2() = assert("\\x -> if x then true else x", "Bool -> Bool")
    fun if3() = assert("\\x -> if x then x else x", "Bool -> Bool")
    fun if4() = assert("\\x -> \\y -> if x then y else x", "Bool -> Bool -> Bool")
    fun if5() = assert("\\x -> \\y -> \\z -> if x then y else z", "Bool -> a -> a -> a")
    fun if6() = nassert<UnableToUnify>("\\x -> if x then x :: String else x")
    fun if7() = nassert<UnableToUnify>("\\x -> if x then x else (x true)")
}
