package se.smasseman.kanonen.core

class SequenceValidator(
    private val outputs: Collection<OutputName>,
    private val inputNames: Collection<InputName>,
    private val sequences: List<Sequence>
) {
    fun validate() {
        val nameList = sequences.map { it.name }.toMutableList()
        val nameSet = nameList.toSet()
        nameSet.forEach { nameList.remove(it) }
        if (nameList.isNotEmpty()) throw RuntimeException(
            "There was more then one sequence with name " + nameList.joinToString(
                ","
            )
        )

        sequences.forEach { sequence ->
            sequence.actionLines.forEach { line ->
                val action = line.action
                validateAction(action, line, sequence)
            }
            sequence.propertyLines.forEach { line ->
                when (line.property) {
                    is TriggerProperty -> validateTrigger(inputNames, line.property, line)
                    is AbortProperty -> validateAbort(inputNames, line.property, line)
                }
            }
        }
    }

    private fun validateAction(
        action: Action,
        line: SequenceActionLine,
        sequence: Sequence
    ) {
        when (action) {
            is ExpectAction -> validateOutput(action.output, line)
            is JumpAction -> validateJumpAction(action, line)
            is SetAction -> validateOutput(action.output, line)
            is WaitAction -> {/* No validation required*/
            }

            is WaitForAction -> validateInput(action.inputName, line)
            is IfInputAction -> validateIfInput(action, line, sequence)
            is IfOutputAction -> validateIfOutput(action, line, sequence)
            is LabelAction -> validateLabel(sequence, action)
        }
    }

    private fun validateIfInput(
        action: IfInputAction,
        line: SequenceActionLine,
        sequence: Sequence
    ) {
        validateInput(action.input, line);
        validateAction(action.action, line, sequence);
    }

    private fun validateIfOutput(
        action: IfOutputAction,
        line: SequenceActionLine,
        sequence: Sequence
    ) {
        validateOutput(action.output, line);
        validateAction(action.action, line, sequence);
    }

    private fun validateLabel(
        sequence: Sequence,
        action: LabelAction
    ) {
        val numberOfLabels = sequence.actionLines
            .filter { it.action is LabelAction }
            .map { it.action as LabelAction }
            .filter { it.labelName == action.labelName }
            .size
        if (numberOfLabels > 1) {
            throw ValidationFailedException("You can not have $numberOfLabels labels with same name '${action.labelName}' in sequence ${sequence.name}")
        }
    }

    private fun validateJumpAction(
        action: JumpAction,
        line: SequenceActionLine
    ) {
        val jumpToSequence = sequences.firstOrNull { it.name == action.sequenceName }
            ?: throw ValidationFailedException("Line $line tries to jump into sequence '${action.sequenceName} that does not exits")
        val possibleLabelNames = jumpToSequence.actionLines.filter { it.action is LabelAction }
            .map { (it.action as LabelAction).labelName }
        if (!possibleLabelNames.contains(action.labelName))
            throw ValidationFailedException("In sequence ${line.sequenceName} at row ${line.lineNumber} command ${line.raw} tries to jump a label '${action.labelName}' but label ${action.labelName} not exits in sequence ${action.sequenceName} the only exiting labels in sequence ${action.sequenceName} are $possibleLabelNames")
    }

    private fun validateOutput(
        output: OutputName,
        line: SequenceActionLine
    ) {
        if (!outputs.contains(output)) {
            throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} output $output is an unknown output. Valid outputs $outputs")
        }
    }

    private fun validateTrigger(
        inputs: Collection<InputName>,
        property: TriggerProperty,
        line: SequenceLine
    ) {
        if (!inputs.contains(property.input)) {
            throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} input ${property.input} is an unknown input. Valid inputs $inputs")
        }
    }

    private fun validateAbort(
        inputs: Collection<InputName>,
        property: AbortProperty,
        line: SequenceLine
    ) {
        if (!inputs.contains(property.input)) {
            throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} input ${property.input} is an unknown input. Valid inputs $inputs")
        }
    }

    private fun validateInput(
        inputName: InputName,
        line: SequenceActionLine
    ) {
        if (!inputNames.contains(inputName)) {
            throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} input $inputName is an unknown input. Valid inputs $inputNames")
        }
    }
}

