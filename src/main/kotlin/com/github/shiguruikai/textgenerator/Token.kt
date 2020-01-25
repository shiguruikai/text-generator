package com.github.shiguruikai.textgenerator

import com.worksap.nlp.sudachi.Morpheme
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

/**
 * @property surface 形態素の文字列
 * @property partOfSpeechId 形態素の品詞ID
 */
class Token internal constructor() : Externalizable {

    var surface: String = ""
        private set

    var partOfSpeechId: Short = 0
        private set

    @Transient
    private var hashCode: Int = 0

    constructor(morpheme: Morpheme) : this() {
        this.surface = morpheme.surface()
        this.partOfSpeechId = morpheme.partOfSpeechId()
        this.hashCode = hash()
    }

    private fun hash(): Int = 31 * partOfSpeechId + surface.hashCode()

    override fun readExternal(oi: ObjectInput) {
        surface = oi.readUTF()
        partOfSpeechId = oi.readShort()
        hashCode = hash()
    }

    override fun writeExternal(oo: ObjectOutput) {
        oo.writeUTF(surface)
        oo.writeShort(partOfSpeechId.toInt())
    }

    override fun equals(other: Any?): Boolean =
        other === this || other is Token &&
                other.hashCode == hashCode &&
                other.partOfSpeechId == partOfSpeechId &&
                other.surface == surface

    override fun hashCode(): Int = hashCode

    override fun toString(): String = "Token(surface='$surface', partOfSpeechId=$partOfSpeechId)"
}
