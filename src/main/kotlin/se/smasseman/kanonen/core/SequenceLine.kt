package se.smasseman.kanonen.core

sealed class SequenceLine(open val sequenceName: SequenceName, open val lineNumber: Int, open val raw: String)

data class SequenceActionLine(
    override val sequenceName: SequenceName,
    override val lineNumber: Int,
    val action: Action,
    override val raw: String
) : SequenceLine(
    sequenceName, lineNumber, raw
)

data class SequencePropertyLine(
    override val sequenceName: SequenceName,
    override val lineNumber: Int,
    val property: Property,
    override val raw: String
) : SequenceLine(
    sequenceName, lineNumber, raw
)
