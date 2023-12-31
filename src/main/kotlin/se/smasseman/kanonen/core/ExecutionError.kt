package se.smasseman.kanonen.core

data class ExecutionError(
    val line: SequenceActionLine,
    val message: String) {

    override fun toString() = "ERROR: Could not execute sequence: ${line.sequenceName} line: ${line.lineNumber} action: ${line.raw} Message: $message"
}
