package io.github.aedans.hm

import com.github.h0tk3y.betterParse.grammar.parseToEnd

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) {
    print(">")
    System.`in`.bufferedReader().lines().forEach {
        val expr = ExprGrammar.parseToEnd(it)
        println(expr)
        val (_, type) = expr.infer(emptyMap())
        println(type.generalize(emptyMap()))
        fresh = 0
        print(">")
    }
}
