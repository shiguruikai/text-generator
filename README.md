# text-generator

マルコフ連鎖でテキストを生成する。

形態素解析器は [Sudachi](https://github.com/WorksApplications/Sudachi) を使用。

## 使い方

形態素解析に必要なSudachiの辞書ファイル `system_full.dic` は自前でビルドするか、
[リポジトリ](https://oss.sonatype.org/content/repositories/snapshots/com/worksap/nlp/sudachi/0.1.1-SNAPSHOT/) から
`sudachi-{バージョンID}-dictionary-full.zip` をダウンロード＆解凍して入手する。

```text 
Usage:  text-generator [options]
            (標準入力から生成する場合)
        text-generator [options] <file>
            (テキストファイルから生成する場合)
        text-generator [options] -token <file>
            (トークンファイルから生成する場合、オプションは -l, -c, -o のみ有効)

Options:
    -h, --help                  このヘルプを表示して終了する
    -s, --settings <file>       設定ファイルを指定
                                （デフォルトはJar内部の sudachi_fulldict.json）
    -d, --dic-dir <dir>         辞書ファイルのディレクトリ
                                （デフォルトはJarと同じディレクトリ）
    -m, --mode [a|b|c]          形態素分割モード
                                （デフォルトは c）
    -l, --limit <num>           生成するテキストの形態素の個数
                                （デフォルトは 100）
    -c, --chain-size <num>      マルコフ連鎖のチェーンサイズ
                                (2 以上を指定する、デフォルトは 3)
    -o, --output-file <file>    生成したテキストをファイルに書き込む
                                （指定が無い場合は標準出力）
    -O, --output-token <file>   トークンファイルを出力して終了する
```
