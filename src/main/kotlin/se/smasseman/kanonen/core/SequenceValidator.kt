package se.smasseman.kanonen.core

import kotlin.RuntimeException

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
                sequence.lines.forEach { line ->
                    when (line.action) {
                        is ExpectAction -> validateOutput(outputs, line.action.output, line)
                        is JumpAction -> validateCallOrGoto(sequences, line.action, line)
                        is SetAction -> validateOutput(outputs, line.action.output, line)
                        is WaitAction -> {}
                        is WaitForAction -> validateInput(inputNames, line.action.inputName, line)
                        is LabelAction -> if (sequence.lines
                                .filter { otherLine -> otherLine != line }
                                .filter { otherLine -> otherLine.action is LabelAction && otherLine.action.labelName == line.action.labelName }
                                .firstOrNull() != null
                        ) throw ValidationFailedException("You can not have to labels with same name '${line.action.labelName}' in sequence ${sequence.name}")
                    }
                }
            }
        }

        private fun validateCallOrGoto(
            sequences: List<Sequence>,
            action: JumpAction,
            line: SequenceLine
        ) {
            val jumpToSequence = sequences.firstOrNull { it.name == action.sequenceName }
                ?: throw RuntimeException("Line $line tries to jump into sequence '${action.sequenceName} that does not exits")
            val possibleLabelNames = jumpToSequence.lines.filter { it.action is LabelAction }
                .map { (it.action as LabelAction).labelName }
            if (!possibleLabelNames.contains(action.labelName))
                throw ValidationFailedException("In sequence ${line.sequenceName} at row ${line.lineNumber} command ${line.raw} tries to jump a label '${action.labelName}' but label ${action.labelName} not exits in sequence ${action.sequenceName} the only exiting labels in sequence ${action.sequenceName} are $possibleLabelNames")
        }

        private fun validateOutput(
            outputs: Collection<OutputName>,
            output: OutputName,
            line: SequenceLine
        ) {
            if (!outputs.contains(output)) {
                throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} output $output is an unknown output. Valid outputs $outputs")
            }
        }

        private fun validateInput(
            inputs: Collection<InputName>,
            inputName: InputName,
            line: SequenceLine
        ) {
            if (!inputs.contains(inputName)) {
                throw ValidationFailedException("Error on line ${line.lineNumber} in sequence ${line.sequenceName} input $inputName is an unknown input. Valid inputs $inputs")
            }
        }
    }

}
