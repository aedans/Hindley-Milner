package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

fun Type.generalize(env: Env): Scheme = Scheme((freeTypeVariables - env.freeTypeVariables).toList(), this)
