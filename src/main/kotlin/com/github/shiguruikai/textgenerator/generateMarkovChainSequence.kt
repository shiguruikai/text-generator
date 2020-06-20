package com.github.shiguruikai.textgenerator

import java.util.LinkedList
import kotlin.random.Random

fun <T : Any> generateMarkovChainSequence(src: List<T>, chainSize: Int, random: Random = Random): Sequence<T> {
    require(chainSize in 2..src.size) { "chainSize out of range: $chainSize" }

    if (src.isEmpty()) return emptySequence()

    val n = chainSize - 1
    val keys = src.takeLast(n).asSequence().plus(src).windowed(n)
    val values = src.asSequence()
    val chain = keys.zip(values).groupBy({ it.first }, { it.second })

    val key = LinkedList(chain.keys.random(random))

    return generateSequence {
        chain[key]?.random(random)?.also {
            key.removeFirst()
            key.addLast(it)
        }
    }
}
