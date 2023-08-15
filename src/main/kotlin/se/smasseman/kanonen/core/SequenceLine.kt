package se.smasseman.kanonen.core

data class SequenceLine(val sequenceName: SequenceName, val lineNumber: Int, val action: Action, val raw: String) {

}
