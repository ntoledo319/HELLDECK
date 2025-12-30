package com.helldeck.content.util

import kotlin.random.Random

class SeededRng(sessionSeed: Long) {
    private var rnd = Random(sessionSeed)
    val random: Random get() = rnd
    fun nextInt(bound: Int): Int = rnd.nextInt(bound)
    fun <T> choice(list: List<T>): T = list[rnd.nextInt(list.size)]
    fun nextDouble(): Double = rnd.nextDouble()
    fun reseed(newSeed: Long) { rnd = Random(newSeed) }
}
