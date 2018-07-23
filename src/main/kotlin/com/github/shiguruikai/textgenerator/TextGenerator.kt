package com.github.shiguruikai.textgenerator

import com.worksap.nlp.sudachi.Tokenizer
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

fun createTokenList(tokenizer: Tokenizer, mode: Tokenizer.SplitMode, inputStream: InputStream): List<String> {
    return inputStream.bufferedReader().useLines {
        it.fold(mutableListOf()) { acc, line ->
            acc.apply {
                tokenizer.tokenize(mode, line).forEach {
                    acc += it.surface()
                }
            }
        }
    }
}

fun <T : Any> generateMarkovChainSequence(src: List<T>, chainSize: Int = 3, random: Random = Random()): Sequence<T> {
    require(chainSize > 1)
    require(chainSize <= src.size)

    if (src.isEmpty()) return emptySequence()

    val chain = mutableMapOf<List<T>, MutableList<T>>()
    val keySize = chainSize - 1
    val keySequence = src.takeLast(keySize).asSequence().plus(src).windowed(keySize)
    val valueSequence = src.asSequence()
    keySequence.zip(valueSequence).forEach { (key, value) ->
        chain.getOrPut(key) { ArrayList() }.add(value)
    }

    //val key = LinkedList(chain.keys.elementAt(random.nextInt(chain.size)))
    val key = LinkedList(src.takeLast(keySize))

    return generateSequence {
        chain[key]?.choice(random)?.also {
            key.removeFirst()
            key.addLast(it)
        }
    }
}
