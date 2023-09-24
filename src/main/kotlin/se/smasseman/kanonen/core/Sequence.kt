package se.smasseman.kanonen.core

data class Sequence(
    val name: SequenceName,
    val lines: List<SequenceLine>
) {
    val actionLines: List<SequenceActionLine>
        get() = lines.filterIsInstance<SequenceActionLine>().toList()
    val propertyLines: List<SequencePropertyLine>
        get() = lines.filterIsInstance<SequencePropertyLine>().toList()
}
