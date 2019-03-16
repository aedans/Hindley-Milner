package io.github.aedans.hm

import arrow.core.Option
import arrow.data.*
import arrow.effects.*
import arrow.effects.extensions.io.applicative.applicative
import arrow.effects.extensions.io.functor.functor
import arrow.effects.extensions.io.monad.monad
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

    fun output(error: InferenceError): IO<Unit> = IO { println(error.message) }

    /**
     * A process which outputs the type of an expression.
     */
    fun output(expr: Expr, type: Polytype): IO<Unit> = IO { println("${toString(expr)} :: $type") }

    /**
     * A process which runs the the REPL.
     */
    fun repl(): OptionT<ForIO, Unit> = input()
            .flatMap(IO.monad()) { input ->
                if (input.isEmpty()) {
                    OptionT.none(IO.applicative())
                } else {
                    val expr = Grammar.parseToEnd(input)
                    val result = expr.infer(Env.empty)
                    fresh = 0
                    OptionT.liftF(IO.functor(), result.fold(
                            { output(it) },
                            { (_, type) -> output(expr, type.generalize(Env.empty)) }
                    ))
                }
            }
            .flatMap(IO.monad()) { repl() }
            .fix()

    /**
     * The main method which launches the REPL.
     */
    @JvmStatic
    fun main(@Suppress("UnusedMainParameter") args: Array<String>) {
        repl().value().fix().unsafeRunSync()
    }
}