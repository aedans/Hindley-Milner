package io.github.aedans.hm

import arrow.*
import arrow.core.Eval.Companion.now
import arrow.recursion.data.Fix
import arrow.typeclasses.Functor

/**
 * The fixed point of [TLCExprF].
 */
typealias TLCMonotype = Fix<ForTLCMonotypeF>

/**
 * An algebraic data type representing a monotype.
 */
@higherkind
sealed class TLCMonotypeF<out Self> : TLCMonotypeFOf<Self> {
    /**
     * A data class representing a variable type.
     *
     * @param name The name of the variable.
     */
    data class Variable(val name: String) : TLCMonotypeF<Nothing>() {
        override fun toString() = name
    }

    /**
     * A data class representing a constant type.
     *
     * @param name The name of the constant.
     */
    data class Constant(val name: String) : TLCMonotypeF<Nothing>() {
        override fun toString() = name
    }

    /**
     * A data class representing type application.
     *
     * @param function The type being applied.
     * @param arg      The argument of the type.
     */
    data class Apply<out Self>(val function: Self, val arg: Self) : TLCMonotypeF<Self>() {
        override fun toString() = "($function) $arg"
    }

    companion object {
        fun variable(name: String) = Fix(Variable(name))
        fun constant(name: String) = Fix(Constant(name))
        fun apply(function: TLCMonotype, arg: TLCMonotype) = Fix(Apply(now(function), now(arg)))
        fun arrow(input: TLCMonotype, output: TLCMonotype) = apply(apply(TLCMonotypeF.constant("->"), input), output)

        /**
         * The singleton primitive boolean type.
         */
        val bool = constant("Bool")
    }
}

/**
 * A functor instance for [TLCMonotypeF].
 */
object TLCMonotypeFFunctor : Functor<ForTLCMonotypeF> {
    override fun <A, B> TLCMonotypeFOf<A>.map(f: (A) -> B) = when (val type = fix()) {
        is TLCMonotypeF.Variable -> type
        is TLCMonotypeF.Constant -> type
        is TLCMonotypeF.Apply -> TLCMonotypeF.Apply(f(type.function), f(type.arg))
    }
}

/**
 * A data class representing a polytype.
 */
data class TLCPolytype(val names: List<String>, val type: TLCMonotype) {
    override fun toString() = "$names => $type"
}

val TLCMonotype.scheme get() = TLCPolytype(emptyList(), this)

fun TLCMonotype.generalize(env: Env) = TLCPolytype(
        freeTypeVariables().filterNot { tVar -> env.get(tVar).let { it != null && it.names.contains(tVar) } },
        this
)

fun TLCPolytype.instantiate() = run {
    val namesP = names.map { TLCMonotypeF.variable(fresh()) }
    val namesZ = (names zip namesP).toMap()
    apply(namesZ, type)
}