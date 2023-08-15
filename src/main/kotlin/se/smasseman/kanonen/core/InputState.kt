package se.smasseman.kanonen.core

enum class InputState {
    ON, OFF;

    fun toggle(): InputState {
        return when (this) {
            ON -> OFF
            OFF -> ON
        }
    }
}