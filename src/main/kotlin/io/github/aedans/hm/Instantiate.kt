package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Scheme.instantiate(): Type = run {
    val absP = abs.map { Type.Var(fresh()) }
    val absZ = (abs zip absP).toMap()
    apply(absZ, type)
}
