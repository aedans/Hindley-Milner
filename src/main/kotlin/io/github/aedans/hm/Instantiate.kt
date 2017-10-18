package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Scheme.instantiate(): Type = run {
    val namesP = names.map { Type.Var(fresh()) }
    val namesZ: Subst = (names zip namesP).toMap()
    apply(namesZ, type)
}
