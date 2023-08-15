package se.smasseman.kanonen.core

import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.io.gpio.digital.DigitalStateChangeListener

class PiInput(name: InputName, val input: DigitalInput) : Input(name) {
    init {
        input.addListener(DigitalStateChangeListener { event ->
            state = when (event.state()) {
                DigitalState.LOW -> InputState.ON
                DigitalState.UNKNOWN -> InputState.OFF
                DigitalState.HIGH -> InputState.OFF
                null -> InputState.OFF
            }
        })
    }
}
