package se.smasseman.kanonen.core

import java.io.File
import java.io.FileNotFoundException
import java.util.*


class PinConfig : LogUtil {

    private fun readConf(resourceName: String): Map<Int, String> {
        val directory = System.getProperty(
            "pins",
            "/Users/jorgensmas/git/smasseman/kanonen/src/main/resources/pins/"
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

}
