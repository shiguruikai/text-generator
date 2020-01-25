package com.github.shiguruikai.textgenerator

import com.worksap.nlp.sudachi.DictionaryFactory
import com.worksap.nlp.sudachi.SudachiCommandLine
import com.worksap.nlp.sudachi.Tokenizer
import com.worksap.nlp.sudachi.Tokenizer.SplitMode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.math.max
import kotlin.system.exitProcess
import java.lang.System.`in` as stdin
import java.lang.System.err as stderr
import java.lang.System.out as stdout

@SuppressFBWarnings("DM_EXIT")
object TextGeneratorCommandLine {

    private const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_RED = "\u001B[31m"

    @JvmStatic
    fun main(args: Array<String>) {
        var settings: String? = null
        var dicDirPath: Path? = null
        var outputTokenPath: Path? = null
        var inputTokenPath: Path? = null
        var outputStream: Lazy<OutputStream> = lazy { stdout }
        var mode = SplitMode.C
        var limit = 100
        var chainSize = 3

        val argsIter = if (args.isNotEmpty()) {
            args.toList()
        } else {
            listOf("--help")
        }.listIterator()

        fun checkOptionArg(name: String) {
            if (!argsIter.hasNext()) {
                stderr.println("${ANSI_RED}error:$ANSI_RESET オプションには引数が必要です $name")
                exitProcess(1)
            }
        }

        loop@ while (argsIter.hasNext()) {
            when (val arg = argsIter.next()) {
                "-h", "--h", "-help", "--help" -> {
                    printHelpMessage()
                    exitProcess(0)
                }
                "-token" -> {
                    checkOptionArg(arg)
                    inputTokenPath = argsIter.next().toPath()
                }
                "-s", "--settings" -> {
                    checkOptionArg(arg)
                    settings = argsIter.next().toPath().toFile().readText()
                }
                "-d", "--dic-dir" -> {
                    checkOptionArg(arg)
                    dicDirPath = argsIter.next().toPath()
                }
                "-m", "--mode" -> {
                    checkOptionArg(arg)
                    mode = when (argsIter.next()) {
                        "a", "A" -> SplitMode.A
                        "b", "B" -> SplitMode.B
                        else -> SplitMode.C
                    }
                }
                "-l", "--limit" -> {
                    checkOptionArg(arg)
                    limit = max(0, argsIter.next().toInt())
                }
                "-c", "--chain-size" -> {
                    checkOptionArg(arg)
                    chainSize = argsIter.next().toInt()
                }
                "-o", "--output-file" -> {
                    checkOptionArg(arg)
                    val path = argsIter.next().toPath()
                    outputStream = lazy { Files.newOutputStream(path, CREATE, APPEND) }
                }
                "-O", "--output-token" -> {
                    checkOptionArg(arg)
                    outputTokenPath = argsIter.next().toPath()
                }
                else -> {
                    if (arg.startsWith('-')) {
                        stderr.println("${ANSI_RED}error:$ANSI_RESET 不明なオプション $arg")
                        exitProcess(1)
                    }
                    argsIter.previous()
                    break@loop
                }
            }
        }

        if (inputTokenPath != null) {
            val tokes = deserialize<List<Token>>(inputTokenPath)
            writeGeneratedText(tokes, chainSize, limit, outputStream.value)
            exitProcess(0)
        }

        if (settings == null) {
            settings = SudachiCommandLine::class.java.getResource("/sudachi_fulldict.json").readText()
        }

        if (dicDirPath == null) {
            val location = javaClass.protectionDomain.codeSource.location
            dicDirPath = Paths.get(location.toURI()).parent
        }

        DictionaryFactory().create(dicDirPath.toString(), settings).use { dict ->
            val tokenizer = dict.create()

            val inputStream = when {
                argsIter.hasNext() -> Files.newInputStream(argsIter.next().toPath())
                stdin.available() != 0 -> stdin
                else -> exitProcess(0)
            }

            val tokens = createTokenList(tokenizer, mode, inputStream)

            if (outputTokenPath != null) {
                serialize(tokens, outputTokenPath)
                exitProcess(0)
            }

            writeGeneratedText(tokens, chainSize, limit, outputStream.value)
        }
    }

    private fun printHelpMessage() {
        println(
            """
            Usage:  text-generator [options]
                        (標準入力から生成する場合)
                    text-generator [options] <file>
                        (テキストファイルから生成する場合)
                    text-generator [options] -token <file>
                        (トークンファイルから生成する場合、オプションは -l, -c, -o のみ有効)

            Options:
                -h, --help                  このヘルプを表示して終了する
                -s, --settings <file>       設定ファイルを指定
                                            （デフォルトは内部の sudachi_fulldict.json）
                -d, --dic-dir <dir>         辞書ファイルのディレクトリ
                                            （デフォルトは同じディレクトリ）
                -m, --mode [a|b|c]          形態素分割モード
                                            （デフォルトは c）
                -l, --limit <num>           生成するテキストの形態素の個数
                                            （デフォルトは 100）
                -c, --chain-size <num>      マルコフ連鎖で考慮する形態素の数
                                            (デフォルトは 3)
                -o, --output-file <file>    生成したテキストの出力ファイル
                                            （指定が無い場合は標準出力）
                -O, --output-token <file>   トークンファイルを出力して終了する
            """.trimIndent()
        )
    }

    private fun writeGeneratedText(tokens: List<Token>, chainSize: Int, limit: Int, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            generateMarkovChainSequence(tokens, chainSize)
                .take(limit)
                .forEach {
                    writer.write(it.surface)
                }

            writer.newLine()
        }
    }

    private fun createTokenList(tokenizer: Tokenizer, mode: SplitMode, inputStream: InputStream): List<Token> {
        return inputStream.bufferedReader().useLines { sequence ->
            sequence.fold(mutableListOf()) { acc, line ->
                tokenizer.tokenize(mode, line).forEach { morpheme ->
                    acc += Token(morpheme)
                }
                return@fold acc
            }
        }
    }

    private fun <T> serialize(any: T, path: Path) {
        val outputStream = DeflaterOutputStream(Files.newOutputStream(path)).buffered()
        ObjectOutputStream(outputStream).use {
            it.writeObject(any)
        }
    }

    private fun <T> deserialize(path: Path): T {
        val inputStream = InflaterInputStream(Files.newInputStream(path)).buffered()
        return ObjectInputStream(inputStream).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as T
        }
    }
}
