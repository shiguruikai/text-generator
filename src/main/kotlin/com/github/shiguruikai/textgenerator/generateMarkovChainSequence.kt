package com.github.shiguruikai.textgenerator

import java.util.LinkedList
import kotlin.random.Random

fun <T : Any> generateMarkovChainSequence(src: List<T>, chainSize: Int, random: Random = Random): Sequence<T> {
    require(chainSize in 2..src.size) { "chainSize out of range: $chainSize" }

    if (src.isEmpty()) return emptySequence()

    val chain = mutableMapOf<List<T>, MutableList<T>>()
    val keySize = chainSize - 1
    val keySequence = src.takeLast(keySize).asSequence().plus(src).windowed(keySize)
    val valueSequence = src.asSequence()
    keySequence.zip(valueSequence).forEach { (key, value) ->
        chain.getOrPut(key) { mutableListOf() }.add(value)
    }

    val key = LinkedList(chain.keys.random(random))

    return generateSequence {
        chain[key]?.random(random)?.also {
            key.removeFirst()
            key.addLast(it)
        }
    }
}
