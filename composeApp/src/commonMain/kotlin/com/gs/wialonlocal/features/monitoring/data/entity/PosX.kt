package com.gs.wialonlocal.features.monitoring.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class PosX(
    val c: Int,
    val f: Int,
    val lc: Int,
    val s: Int,
    val sc: Int,
    val t: Int,
    val x: Double,
    val y: Double,
    val z: Int
)