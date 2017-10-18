package io.github.aedans.hm

/**
 * Created by Aedan Smith.
 */

var fresh = 0
fun fresh() = "${fresh++}"

typealias Env = Map<String, Scheme>
