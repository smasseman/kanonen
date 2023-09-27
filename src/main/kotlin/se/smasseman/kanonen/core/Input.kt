package se.smasseman.kanonen.core

import java.util.*

open class Input(val name: InputName) {

    private val listeners = LinkedList<InputListener>()
    var state : InputState = InputState.ON
        protected set(newState) {
            field = newState
            listeners.forEach { it.inputUpdated(name, newState) }
        }

    fun addListener(listener: InputListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: InputListener) {
        listeners.remove(listener)
    }

    override fun toString() = name.name
}

fun interface InputListener {
    fun inputUpdated(inputName: InputName, inputState: InputState)
}
