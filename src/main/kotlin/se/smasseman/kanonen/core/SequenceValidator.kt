package se.smasseman.kanonen.core

class SequenceValidator {
    companion object {
        fun validate(outputs: Collection<OutputName>, inputNames: Collection<InputName>, sequences: List<Sequence>) {
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
                    when (line.action) {
                        is ExpectAction -> validateOutput(outputs, line.action.output, line)
                        is JumpAction -> validateCallOrGoto(sequences, line.action, line)
                        is SetAction -> validateOutput(outputs, line.action.output, line)
                        is WaitAction -> {/* No validation required*/}
                        is WaitForAction -> validateInput(inputNames, line.action.inputName, line)
                        is LabelAction -> if (sequence.actionLines
                                .filter { otherLine -> otherLine != line }
                                .filter { otherLine -> otherLine.action is LabelAction && otherLine.action.labelName == line.action.labelName }
                                .firstOrNull() != null
                        ) throw ValidationFailedException("You can not have to labels with same name '${line.action.labelName}' in sequence ${sequence.name}")
                    }
                }
                sequence.propertyLines.forEach { line ->
                    when (line.property) {
                        is TriggerProperty -> validateTrigger(inputNames, line.property, line)
                    }
                }
            }
        }

        private fun validateCallOrGoto(
            sequences: List<Sequence>,
            action: JumpAction,
            line: SequenceActionLine
        ) {
            val jumpToSequence = sequences.firstOrNull { it.name == action.sequenceName }
                ?: throw RuntimeException("Line $line tries to jump into sequence '${action.sequenceName} that does not exits")
            val possibleLabelNames = jumpToSequence.actionLines.filter { it.action is LabelAction }
                .map { (it.action as LabelAction).labelName }
            if (!possibleLabelNames.contains(action.labelName))
                throw ValidationFailedException("In sequence ${line.sequenceName} at row ${line.lineNumber} command ${line.raw} tries to jump a label '${action.labelName}' but label ${action.labelName} not exits in sequence ${action.sequenceName} the only exiting labels in sequence ${action.sequenceName} are $possibleLabelNames")
        }

        private fun validateOutput(
            outputs: Collection<OutputName>,
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

        private fun validateInput(
            inputs: Collection<InputName>,
            inputName: InputName,
            line: SequenceActionLine
        ) {
            if (!inputs.contains(inputName)) {
                throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} input $inputName is an unknown input. Valid inputs $inputs")
            }
        }
    }

}
