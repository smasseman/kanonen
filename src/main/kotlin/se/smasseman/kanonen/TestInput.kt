package se.smasseman.kanonen

import se.smasseman.kanonen.core.InputName
import se.smasseman.kanonen.core.InputState
import se.smasseman.kanonen.core.PiContext

fun main(args: Array<String>) {
    val input = PiContext.createInput(args[0].toInt(), InputName(args[0]))
    input.addListener { inputName: InputName, inputState: InputState ->
        println("$inputName $inputState")
    }
    var line = readln()
    while (line != "q") {
        line = readln()
    }

}