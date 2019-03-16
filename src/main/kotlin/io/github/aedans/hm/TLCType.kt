package io.github.aedans.hm

/**
 * An algebraic data type representing a typed lambda calculus type.
 */
sealed class TLCType {
    /**
     * An algebraic data type representing a monotype.
     */
    sealed class Mono : TLCType() {
        /**
         * A data class representing a variable type.
         *
         * @param name The name of the variable.
         */
        data class Var(val name: String) : Mono() {
            override fun toString() = name
        }

        /**
         * A data class representing a constant type.
         *
         * @param name The name of the constant.
         */
        data class Const(val name: String) : Mono() {
            override fun toString() = name
        }

        /**
         * A data class representing type application.
         *
         * @param function The type being applied.
         * @param arg      The argument of the type.
         */
        data class Apply(val function: Mono, val arg: Mono) : Mono() {
            override fun toString() = "($function) $arg"
        }

        /**
         * A data class representing the function type.
         *
         * @param input  The type of the input of the function.
         * @param output The type of the output of the function.
         */
        data class Arrow(val input: Mono, val output: Mono)

        companion object {
            /**
             * The singleton primitive boolean type.
             */
            val bool = Const("Bool")
        }
    }

    /**
     * A data class representing a polytype.
     */
    data class Poly(val names: List<String>, val type: Mono) : TLCType() {
        override fun toString() = "$names => $type"
    }
}

val TLCType.Mono.Arrow.type get() = TLCType.Mono.Apply(TLCType.Mono.Apply(TLCType.Mono.Const("->"), input), output)

val TLCType.Mono.scheme get() = TLCType.Poly(emptyList(), this)

fun TLCType.Mono.generalize(env: Env) = TLCType.Poly(
        freeTypeVariables.filterNot { tVar -> env.get(tVar).let { it != null && it.names.contains(tVar) } },
        this
)

fun TLCType.Poly.instantiate() = run {
    val namesP = names.map { TLCType.Mono.Var(fresh()) }
    val namesZ = (names zip namesP).toMap()
    apply(namesZ, type)
}