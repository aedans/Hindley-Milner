package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

var fresh = 'a'
fun fresh() = "${fresh++}"

typealias Env = Map<Expr.Var, Scheme>
