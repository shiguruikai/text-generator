package com.github.shiguruikai.textgenerator

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

fun String.toPath(): Path = if (startsWith('~')) {
    Paths.get(System.getProperty("user.home") + substring(1))
} else {
    Paths.get(this)
}

fun <T> List<T>.choice(random: Random = Random(), bound: Int = size): T = get(random.nextInt(bound))
