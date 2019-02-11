package com.github.shiguruikai.textgenerator

import java.nio.file.Path
import java.nio.file.Paths

fun String.toPath(): Path = if (startsWith('~')) {
    Paths.get(System.getProperty("user.home") + substring(1))
} else {
    Paths.get(this)
}
