package io.github.aedans.hm

import arrow.*
import arrow.core.Eval.Companion.now
import arrow.recursion.data.Fix
import arrow.typeclasses.Functor

/**
 * The fixed point of [ExprF].
 */
typealias Monotype = Fix<ForMonotypeF>

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
    data class Variable(val name: String) : MonotypeF<Nothing>() {
        override fun toString() = name
    }

    /**
     * A data class representing a constant type.
     *
     * @param name The name of the constant.
     */
    data class Constant(val name: String) : MonotypeF<Nothing>() {
        override fun toString() = name
    }

    /**
     * A data class representing type application.
     *
     * @param function The type being applied.
     * @param arg      The argument of the type.
     */
    data class Apply<out Self>(val function: Self, val arg: Self) : MonotypeF<Self>() {
        override fun toString() = "($function) $arg"
    }

    companion object {
        fun variable(name: String) = Fix(Variable(name))
        fun constant(name: String) = Fix(Constant(name))
        fun apply(function: Monotype, arg: Monotype) = Fix(Apply(now(function), now(arg)))
        fun arrow(input: Monotype, output: Monotype) = apply(apply(MonotypeF.constant("->"), input), output)

        /**
         * The singleton primitive boolean type.
         */
        val bool = constant("Bool")
    }
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
 * A data class representing a polytype.
 */
data class Polytype(val names: List<String>, val type: Monotype) {
    override fun toString() = "$names => $type"
}

val Monotype.scheme get() = Polytype(emptyList(), this)

fun Monotype.generalize(env: Env) = Polytype(
        freeTypeVariables().filterNot { tVar -> env.get(tVar).let { it != null && it.names.contains(tVar) } },
        this
)

fun Polytype.instantiate() = run {
    val namesP = names.map { MonotypeF.variable(fresh()) }
    val namesZ = (names zip namesP).toMap()
    apply(namesZ, type)
}