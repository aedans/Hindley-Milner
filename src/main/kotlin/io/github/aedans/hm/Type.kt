package io.github.aedans.hm

import arrow.*
import arrow.core.*
import arrow.core.Eval.Companion.now
import arrow.core.extensions.eval.monad.monad
import arrow.recursion.typeclasses.*
import arrow.typeclasses.*

/**
 * The fixed point of [MonotypeF].
 */
typealias Monotype<T> = Kind<T, ForMonotypeF>

/**
 * An algebraic data type representing a monotype.
 */
@higherkind
sealed class MonotypeF<out Self> : MonotypeFOf<Self> {
    /**
     * A data class representing a variable type.
     *
     * @param name The name of the variable.
     */
    data class Variable(val name: String) : MonotypeF<Nothing>()

    /**
     * A data class representing a constant type.
     *
     * @param name The name of the constant.
     */
    data class Constant(val name: String) : MonotypeF<Nothing>()

    /**
     * A data class representing type application.
     *
     * @param function The type being applied.
     * @param arg      The argument of the type.
     */
    data class Apply<out Self>(val function: Self, val arg: Self) : MonotypeF<Self>()
}

/**
 * A factory for monotypes.
 */
class MonotypeFactory<T>(corecursive: Corecursive<T>) : Corecursive<T> by corecursive, Functor<ForMonotypeF> by MonotypeFFunctor {
    fun variable(name: String) =
            embedT(MonotypeF.Variable(name))

    fun constant(name: String) =
            embedT(MonotypeF.Constant(name))

    fun apply(function: Monotype<T>, arg: Monotype<T>) =
            embedT(MonotypeF.Apply(now(function), now(arg)))

    fun arrow(input: Monotype<T>, output: Monotype<T>) =
            constant("->").flatMap { arrow -> apply(arrow, input).flatMap { arg -> apply(arg, output) } }

    val bool = constant("Bool")
}

/**
 * A functor instance for [MonotypeF].
 */
object MonotypeFFunctor : Functor<ForMonotypeF> {
    override fun <A, B> MonotypeFOf<A>.map(f: (A) -> B) = when (val type = fix()) {
        is MonotypeF.Variable -> type
        is MonotypeF.Constant -> type
        is MonotypeF.Apply -> MonotypeF.Apply(f(type.function), f(type.arg))
    }
}

/**
 * A show instance for [Monotype].
 */
class MonotypeShow<T>(recursive: Recursive<T>) : Show<Monotype<T>>, Recursive<T> by recursive {
    override fun Monotype<T>.show() =
            MonotypeFFunctor.cata<ForMonotypeF, Pair<String, Boolean>>(this) {
                when (val type = it.fix()) {
                    is MonotypeF.Variable -> now(type.name to true)
                    is MonotypeF.Constant -> now(type.name to true)
                    is MonotypeF.Apply -> Eval.monad().binding {
                        val (function, atomic1) = type.function.bind()
                        val (arg, atomic2) = type.arg.bind()
                        when (function) {
                            "->" -> (if (atomic2) "$arg ->" else "($arg) ->") to true
                            else -> (if (atomic1) "$function $arg" else "($function) $arg") to false
                        }
                    }.fix()
                }
            }.first
}

/**
 * A data class representing a polytype.
 */
@higherkind
data class Polytype<out T>(val names: List<String>, val type: Monotype<T>) : PolytypeOf<T>

class PolytypeShow<T>(recursive: Recursive<T>) : Show<Polytype<T>> {
    val monotypeShow = MonotypeShow(recursive)

    override fun Polytype<T>.show() = monotypeShow.run {
        val type = type.show()
        if (names.isEmpty()) type else "$names => $type"
    }
}

/**
 * Converts a monotype to a polytype.
 */
fun <T> poly(type: Monotype<T>): Polytype<T> = Polytype(emptyList(), type)

/**
 * Generalizes a monotype given an env.
 */
fun <T> Recursive<T>.generalize(type: Monotype<T>, env: Env<T>): Polytype<T> = Polytype(
        freeTypeVariables(type).filterNot { variable ->
            env.get(variable).let { it != null && it.names.contains(variable) }
        },
        type
)

/**
 * Instantiates a polytype.
 */
fun <T> Birecursive<T>.instantiate(type: Polytype<T>): Monotype<T> = MonotypeFactory(this).run {
    val namesP = type.names.map { variable(fresh()).value() }
    val namesZ = (type.names zip namesP).toMap()
    apply(namesZ, type.type)
}