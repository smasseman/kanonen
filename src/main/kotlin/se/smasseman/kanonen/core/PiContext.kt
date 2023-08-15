package se.smasseman.kanonen.core

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalInputProvider
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.platform.Platform
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

            val ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id(name)
                .name(name)
                .address(address)
                .shutdown(DigitalState.HIGH)
                .initial(DigitalState.HIGH)
                .provider("pigpio-digital-output")
                //.provider("raspberrypi-digital-output") // Inget händer. funkar inte

            logger().info("Providers:")
            pi4j.providers().all.forEach {
                logger().info(it.toString())
            }

            val led = pi4j.create(ledConfig);
            logger().info("Created $led with provider ${led.provider()}")
            return PiOutput(
                led
            )
        }

        fun createInput(address: Int, name: InputName): PiInput = PiInput(
            name,
            pi4j.digitalInput<DigitalInputProvider>().create(address, name.name, name.name)
        )

    }

}