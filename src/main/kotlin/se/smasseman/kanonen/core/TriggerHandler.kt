package se.smasseman.kanonen.core

import java.util.*
import kotlin.concurrent.thread

class TriggerHandler(val runner: SequenceRunner, val inputs: Map<InputName, Input>) {

    private data class Registration(val input: Input, val listener: InputListener)

    private val listeners : LinkedList<Registration> = LinkedList()

    fun update(sequences: List<Sequence>) {
            listeners.forEach { it.input.removeListener(it.listener) }
            sequences.forEach { sequence ->
                sequence.propertyLines.forEach { p ->
                    if (p.property is TriggerProperty) {
                        val triggerInputName: InputName = p.property.input
                        val triggerState = p.property.inputState
                        val input = inputs[triggerInputName]
                            ?: throw IllegalStateException("Can not create trigger for input $triggerInputName because it does not exist")
                        val listener = fun(_: InputName, state: InputState) {
                            if (triggerState == state) {
                                thread {
                                    runner.run(sequence.name)
                                }
                            }
                        }
                        input.addListener(listener)
                        listeners.add(Registration(input, listener))
                    }
                }
            }
        }
}
