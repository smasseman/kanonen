package se.smasseman.kanonen.core

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.*
import com.pi4j.plugin.mock.platform.MockPlatform
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInputProvider
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutputProvider

class PiContext : LogUtil {

    companion object : LogUtil {
        val pi4j: Context =
            System.getProperties()["os.name"].let { os ->
                if (os == "Mac OS X") {
                    Pi4J.newContextBuilder()
                        .add(MockPlatform())
                        .add(
                            MockDigitalInputProvider.newInstance(),
                            MockDigitalOutputProvider.newInstance()
                        )
                        .build()
                } else {
                    Pi4J.newAutoContext()
                }
            }.apply {
                logger().info("Running with output provider " + digitalOutput<DigitalOutputProvider>())
            }

        fun createOutput(address: Int, name: String): PiOutput {
            //val output = pi4j.digitalOutput<DigitalOutputProvider>().create<DigitalOutput>(address, name, name)

            val config = DigitalOutput.newConfigBuilder(pi4j)
                .id(name)
                .name(name)
                .address(address)
                .shutdown(DigitalState.HIGH)
                .initial(DigitalState.HIGH)
                .provider("pigpio-digital-output")

            val piOutput = pi4j.create(config);
            logger().info("Created $piOutput with provider ${piOutput.provider()}")
            return PiOutput(piOutput)
        }

        fun createInput(address: Int, name: InputName): PiInput {
            //val input = pi4j.digitalInput<DigitalInputProvider>().create<DigitalInput>(address, name.name, name.name)
            val config = DigitalInput.newConfigBuilder(pi4j)
                .id(name.name)
                .name(name.name)
                .address(address)
                .pull(PullResistance.PULL_UP)
                .provider("pigpio-digital-input")

            val piInput = pi4j.create(config)
            logger().info("Created $piInput with provider ${piInput.provider()}")
            return PiInput(name, piInput)
        }

    }

}