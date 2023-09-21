package se.smasseman.kanonen.core

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.io.gpio.digital.PullResistance
import com.pi4j.plugin.mock.platform.MockPlatform
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInput
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInputProvider
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutputProvider
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

class PiContext : LogUtil {

    companion object : LogUtil {
        private var scheduler: ScheduledExecutorService? = null
        private val osx = System.getProperties()["os.name"] == "Mac OS X"
        private val outputProviderName = if (osx) "mock-digital-output" else "pigpio-digital-output"
        private val inputProviderName = if (osx) "mock-digital-input" else "pigpio-digital-input"
        val pi4j: Context = if (osx)
            Pi4J.newContextBuilder()
                .add(MockPlatform())
                .add(
                    MockDigitalInputProvider.newInstance(),
                    MockDigitalOutputProvider.newInstance()
                )
                .build()
        else
            Pi4J.newAutoContext()

        fun createOutput(address: Int, name: String): PiOutput {
            val config = DigitalOutput.newConfigBuilder(pi4j)
                .id(name)
                .name(name)
                .address(address)
                .shutdown(DigitalState.HIGH)
                .initial(DigitalState.HIGH)
                .provider(outputProviderName)

            val piOutput = pi4j.create(config);
            logger().info("Created $piOutput with provider ${piOutput.provider()}")
            return PiOutput(piOutput)
        }

        fun createInput(address: Int, name: InputName): PiInput {
            val config = DigitalInput.newConfigBuilder(pi4j)
                .id(name.name)
                .name(name.name)
                .address(address)
                .pull(PullResistance.PULL_UP)
                .provider(inputProviderName)

            val piInput = pi4j.create(config)
            logger().info("Created ${piInput.javaClass} $piInput with provider ${piInput.provider()}")
            if (piInput is MockDigitalInput) {
                addMockInput(piInput)
            }
            return PiInput(name, piInput)
        }

        private fun addMockInput(input: MockDigitalInput) {
            val scheduler = this.scheduler ?: Executors.newSingleThreadScheduledExecutor()
            this.scheduler = scheduler

            val path = File("/tmp/input-" + input.name).toPath()
            val action = {
                if (path.exists()) {
                    val content = Files.readString(path).trim()
                    when (content) {
                        "1" -> input.mockState(DigitalState.HIGH)
                        "0" -> input.mockState(DigitalState.LOW)
                    }
                }
            }
            scheduler.scheduleAtFixedRate(action, 400, 100, TimeUnit.MILLISECONDS)
        }

    }

}