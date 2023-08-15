package se.smasseman.kanonen.core

fun interface OutputStateListener {
    fun changed(outputName: OutputName, outputState: OutputState)
}
