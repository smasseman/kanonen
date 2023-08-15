package se.smasseman.kanonen.core

class MockInput(name: InputName) : Input(name) {

    fun set(state: InputState) {
        super.state = state
    }

}
