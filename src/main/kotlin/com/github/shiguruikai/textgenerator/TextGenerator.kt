package com.github.shiguruikai.textgenerator

import com.worksap.nlp.sudachi.Tokenizer
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

fun createTokenList(tokenizer: Tokenizer, mode: Tokenizer.SplitMode, inputStream: InputStream): List<Token> {
    return inputStream.bufferedReader().useLines {
        it.fold(mutableListOf()) { acc, line ->
            acc.apply {
                tokenizer.tokenize(mode, line).forEach {
                    acc += Token(it)
                }
            }
        }
    }
}

fun <T> createMarkovChain(src: List<T>, chainSize: Int = 3): Map<List<T>, List<T>> {
    require(chainSize in 2..src.size) { "チェーンサイズが範囲外です。" }

    val chain = mutableMapOf<List<T>, MutableList<T>>()
    val keySize = chainSize - 1
    val keySequence = src.takeLast(keySize).asSequence().plus(src).windowed(keySize)
    val valueSequence = src.asSequence()
    keySequence.zip(valueSequence).forEach { (key, value) ->
        chain.getOrPut(key) { ArrayList() }.add(value)
    }

    return chain
}

fun <T : Any> generateMarkovChainSequence(src: List<T>, chainSize: Int = 3, random: Random = Random()): Sequence<T> {
    if (src.isEmpty()) return emptySequence()
    if (src.size == 1) return generateSequence { src.single() }

    val chain = createMarkovChain(src, chainSize)
    val key = LinkedList(chain.keys.first())

    return generateSequence {
        chain[key]?.choice(random)?.also {
            key.removeFirst()
            key.addLast(it)
        }
    }
}
