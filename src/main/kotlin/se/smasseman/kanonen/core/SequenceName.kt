package se.smasseman.kanonen.core

import java.lang.IllegalArgumentException

data class SequenceName(val name: String) {

    init {
        val validString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-_"
        val valid = validString.toCharArray()
        name.toCharArray().forEach { c : Char ->
            if( !valid.contains(c)) {
                throw IllegalArgumentException("$name is not a legal sequence name because it contains '$c' but names may only contain $validString")
            }
        }
    }

    override fun toString() = name
}
