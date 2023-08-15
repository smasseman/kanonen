package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MockInputTest {

    @Test
    fun set() {
        val input = MockInput(InputName("IN"))
        var callCounter = 0
        var callName : InputName? = null
        var callState : InputState? = null
        input.addListener { inputName, inputState ->
            callCounter++
            callName = inputName
            callState = inputState
        }

        input.set(InputState.ON)
        input.set(InputState.OFF)
        input.set(InputState.ON)

        assertThat(callCounter).isEqualTo(3)
        assertThat(callName).isEqualTo(input.name)
        assertThat(callState).isEqualTo(InputState.ON)
    }
}