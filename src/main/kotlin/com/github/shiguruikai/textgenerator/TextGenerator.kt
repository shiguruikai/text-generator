package com.github.shiguruikai.textgenerator

import com.worksap.nlp.sudachi.Tokenizer
import java.io.InputStream
import java.util.LinkedList
import kotlin.random.Random

fun createTokenList(tokenizer: Tokenizer, mode: Tokenizer.SplitMode, inputStream: InputStream): List<Token> {
    return inputStream.bufferedReader().useLines { sequence ->
        sequence.fold(mutableListOf()) { acc, line ->
            tokenizer.tokenize(mode, line).forEach { morpheme ->
                acc += Token(morpheme)
            }
            return@fold acc
        }
    }
}

fun <T> createMarkovChain(src: List<T>, chainSize: Int = 3): Map<List<T>, List<T>> {
    require(chainSize in 2..src.size) { "chainSize out of range: $chainSize" }

    val chain = mutableMapOf<List<T>, MutableList<T>>()
    val keySize = chainSize - 1
    val keySequence = src.takeLast(keySize).asSequence().plus(src).windowed(keySize)
    val valueSequence = src.asSequence()
    keySequence.zip(valueSequence).forEach { (key, value) ->
        chain.getOrPut(key) { mutableListOf() }.add(value)
    }

    return chain
}

fun <T : Any> generateMarkovChainSequence(src: List<T>, chainSize: Int = 3, random: Random = Random): Sequence<T> {
    if (src.isEmpty()) return emptySequence()

    val chain = createMarkovChain(src, chainSize)
    val key = LinkedList(chain.keys.random(random))

    return generateSequence {
        chain[key]?.random(random)?.also {
            key.removeFirst()
            key.addLast(it)
        }
    }
}
