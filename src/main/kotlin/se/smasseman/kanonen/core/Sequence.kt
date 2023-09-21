package se.smasseman.kanonen.core

data class Sequence(
    val name: SequenceName,
    val propertyLines: List<SequencePropertyLine>,
    val actionLines: List<SequenceActionLine>) {
}