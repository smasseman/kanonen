package se.smasseman.kanonen.core

import com.pi4j.io.IO
import com.pi4j.io.exception.IONotFoundException
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInput
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class PinConfig : LogUtil {

    private fun readConf(resourceName: String): Map<Int, String> {
        val directory = System.getProperty(
            "pins",
            "/Users/jorgensmas/git/smasseman/ktor-sample/src/main/resources/pins/"
        )
        with(Properties()) {
            val file : File = File("$directory/$resourceName").absoluteFile
            if( !file.canRead() ) {
                throw FileNotFoundException("Could not find file $file")
            }
            return file.inputStream()
                .use { stream ->
                    load(stream)
                    entries.map { entry ->
                        val k: Int = (entry.key as String).toInt()
                        val v: String = entry.value as String
                        k to v
                    }.toMap()
                }
        }
    }

    val outputs: Map<OutputName, Output> = readConf("output.properties")
        .map { PiContext.createOutput(it.key, it.value) }
        .associateBy { it.name() }

    val inputs: Map<InputName, Input> = readConf("input.properties")
        .map { PiContext.createInput(it.key, InputName(it.value)) }
        .associateBy { it.name }

    init {
        try {
            val x: IO<*, *, *> = PiContext.pi4j.getIO("MICRO1")
            if (x is MockDigitalInput) {
                var s = InputState.OFF
                val action = Runnable {
                    s = s.toggle()
                    x.mockState(
                        when (s) {
                            InputState.ON -> DigitalState.HIGH
                            InputState.OFF -> DigitalState.LOW
                        }
                    )
                }
                Executors.newSingleThreadScheduledExecutor()
                    .scheduleWithFixedDelay(action, 5, 4, TimeUnit.SECONDS)
            }
        } catch (e: IONotFoundException) {
            logger().info("Failed to fipple with mock input $e")
        }
    }

}
