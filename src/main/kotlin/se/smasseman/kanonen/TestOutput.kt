package se.smasseman.kanonen

import se.smasseman.kanonen.core.OutputState
import se.smasseman.kanonen.core.PiContext

fun main(args: Array<String>) {
    val out = PiContext.createOutput(args[0].toInt(), "test")
    var state: OutputState = OutputState.OFF
    println("Press enter to toggle. Current state is " + out.get())
    var line = readln()
    while (line != "q") {
        state = state.toggle()
        println("Set $state $out")
        out.set(state)
        line = readln()
    }

}