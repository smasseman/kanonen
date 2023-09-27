package se.smasseman.kanonen.core

import java.util.*

class AbortHandler(runner: SequenceRunner, val inputs: Map<InputName, Input>) {

    init {
        runner.addExecutionListener {
            registrations.find { it.input.state == it.state }?.run {
                throw SequenceAbortedException("$input is $state")
            }
        }
    }

    private data class Registration(val input: Input, val state: InputState)

    private val registrations: LinkedList<Registration> = LinkedList()

    fun update(sequences: List<Sequence>) {
        registrations.clear()
        sequences.forEach { sequence ->
            sequence.propertyLines.forEach { p ->
                if (p.property is AbortProperty) {
                    val abortInputName: InputName = p.property.input
                    val triggerState = p.property.inputState
                    val input = inputs[abortInputName]
                        ?: throw IllegalStateException("Can not configure abort for input $abortInputName because it does not exist. Possible inputs: ${inputs.keys}")
                    registrations.add(Registration(input, triggerState))
                }
            }
        }
    }
}
