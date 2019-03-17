package io.github.aedans.hm

import arrow.core.Option
import arrow.data.*
import arrow.effects.*
import arrow.effects.extensions.io.applicative.applicative
import arrow.effects.extensions.io.functor.functor
import arrow.effects.extensions.io.monad.monad
import arrow.recursion.data.*
import arrow.recursion.extensions.mu.birecursive.birecursive
import arrow.recursion.typeclasses.*
import com.github.h0tk3y.betterParse.grammar.parseToEnd

/**
 * The main class which starts the REPL.
 */
object Repl {
    /**
     * A process which reads input from the user, or if there is no more input.
     */
    fun input(): OptionT<ForIO, String> = OptionT(IO {
        print(">")
        Option.fromNullable(readLine())
    })

    /**
     * A process which outputs an error.
     */
    fun output(error: InferenceError): IO<Unit> = IO { println(error.message) }

    /**
     * A process which outputs the type of an expression.
     */
    fun <T> Recursive<T>.output(expr: Expr<T>, type: Polytype<T>): IO<Unit> = IO {
        val expr = ExprShow(this).run { expr.show() }
        val type = PolytypeShow(this).run { type.show() }
        println("$expr :: $type")
    }

    /**
     * A process which runs the the REPL.
     */
    fun <T> Birecursive<T>.repl(grammar: Grammar<T>): OptionT<ForIO, Unit> = input()
            .flatMap(IO.monad()) { input ->
                if (input.isEmpty()) {
                    OptionT.none(IO.applicative())
                } else {
                    val expr = grammar.parseToEnd(input)
                    val result = infer(expr, Env.empty)
                    fresh = 0
                    OptionT.liftF(IO.functor(), result.fold(
                            { output(it) },
                            { (_, type) -> output(expr, generalize(type, Env.empty)) }
                    ))
                }
            }
            .flatMap(IO.monad()) { repl(grammar) }
            .fix()

    /**
     * The main method which launches the REPL.
     */
    @JvmStatic
    fun main(@Suppress("UnusedMainParameter") args: Array<String>) {
        with(Mu.birecursive()) {
            val grammar = Grammar(MonotypeFactory(this), ExprFactory(this))
            repl(grammar).value().fix().unsafeRunSync()
        }
    }
}