package se.smasseman.kanonen.core

import java.util.regex.Pattern

data class OutputName(val name: String) {

    companion object {
        val PATTERN: Pattern = Pattern.compile("[A-Z_0-9]+")
    }

    init {
        require(PATTERN.matcher(name).matches()) { "$name is an illegal output name." }
    }

    override fun toString() = name

}
