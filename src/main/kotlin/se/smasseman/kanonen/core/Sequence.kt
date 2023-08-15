package se.smasseman.kanonen.core

data class Sequence(
    val name: SequenceName,
    val properties: Map<String, String>,
    val lines: List<SequenceLine>) {
}