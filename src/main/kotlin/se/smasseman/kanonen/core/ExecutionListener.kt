package se.smasseman.kanonen.core

fun interface ExecutionListener {
    fun execute(line: SequenceLine)
}
