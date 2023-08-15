package se.smasseman.kanonen.core

import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalState

class PiOutput(private val output: DigitalOutput) : Output, LogUtil {
    override fun name(): OutputName = OutputName(output.name)

    override fun set(state: OutputState) {
        output.state(
            when (state) {
                OutputState.ON -> DigitalState.HIGH
                OutputState.OFF -> DigitalState.LOW
            }
        )
        logger().info("$output is now ${output.state()}")
    }

    override fun get(): AnyOutputState {
        val state = output.state()
        return when (state) {
            DigitalState.HIGH -> OutputState.ON
            DigitalState.UNKNOWN -> AnyOutputState.UNKNOWN
            DigitalState.LOW -> OutputState.OFF
            null -> AnyOutputState.UNKNOWN
        }
    }

    override fun toString(): String {
        return this.javaClass.simpleName + "[id=${output.id} address=${output.address()} $output"
    }
}
