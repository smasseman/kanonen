package se.smasseman.kanonen.core

import java.util.regex.Pattern

data class InputName(val name: String) {

    companion object {
        val PATTERN: Pattern = Pattern.compile("[A-Z_0-9]+")
    }

    init {
        require(PATTERN.matcher(name).matches()) { "$name is an illegal input name." }
    }

    override fun toString() = name

}
