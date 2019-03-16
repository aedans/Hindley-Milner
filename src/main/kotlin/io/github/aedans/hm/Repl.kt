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

    /**
     * A process which outputs the type of an expression.
     */
    fun output(expr: TLCExpr, type: TLCType): IO<Unit> = IO { println("$expr :: $type") }

    /**
     * A process which runs the the REPL.
     */
    fun repl(): OptionT<ForIO, Unit> = input()
            .flatMap(IO.monad()) { input ->
                if (input.isEmpty()) {
                    OptionT.none(IO.applicative())
                } else {
                    val expr = Grammar.parseToEnd(input)
                    val (_, type) = expr.infer(Env.empty)
                    fresh = 0
                    OptionT.liftF(IO.functor(), output(expr, type))
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