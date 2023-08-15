package se.smasseman.kanonen.core

sealed class AnyOutputState {
    object UNKNOWN : AnyOutputState()

    override fun toString(): String = javaClass.simpleName
}

sealed class OutputState : AnyOutputState() {
    object ON : OutputState()
    object OFF : OutputState()
    fun toggle() : OutputState {
        return when(this) {
            ON -> OFF;
            OFF -> ON;
        }
    }
}