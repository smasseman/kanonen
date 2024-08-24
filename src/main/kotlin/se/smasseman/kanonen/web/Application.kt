package se.smasseman.kanonen.web

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import se.smasseman.kanonen.core.*
import se.smasseman.kanonen.web.plugins.configureRouting
import se.smasseman.kanonen.web.plugins.configureSerialization
import se.smasseman.kanonen.web.plugins.configureSockets
import java.io.File

object KanonenState {

    val pinConfig = PinConfig()
    val triggerHandler: TriggerHandler
    val abortHandler: AbortHandler
    private var reader: SequenceDirectoryReader =
        SequenceDirectoryReader(
            File(
                System.getProperty(
                    "sequences",
                    "/Users/jorgensmas/git/smasseman/kanonen/sequences"
                )
            )
        )

    val runner: SequenceRunner
    var sequences: List<Sequence> = listOf()
        set(s) {
            field = s
            triggerHandler.update(s)
            abortHandler.update(s)
        }


    init {
        val sequenceProvider = object : SequenceProvider {
            override fun get(): List<Sequence> {
                return sequences
            }
        }
        runner =
            SequenceRunner(pinConfig.outputs, pinConfig.inputs, sequenceProvider)
        triggerHandler = TriggerHandler(runner, pinConfig.inputs)
        abortHandler = AbortHandler(runner, pinConfig.inputs)
        sequences = reader.readDirectory()
    }

    fun runSequence(sequenceName: SequenceName) {
        runner.run(sequenceName)
    }

    fun updateSequence(sequenceName: SequenceName, code: String) {
        reader.update(sequenceName, code)
        sequences = reader.readDirectory()
    }

    fun deleteSequence(sequenceName: SequenceName) {
        reader.delete(sequenceName)
        sequences = reader.readDirectory()
    }

    fun newSequence(sequenceName: SequenceName) {
        reader.create(sequenceName)
        sequences = reader.readDirectory()
    }

    fun validateSequences() {
        SequenceValidator(
            pinConfig.outputs.keys,
            pinConfig.inputs.keys,
            sequences
        ).validate()
    }
}

fun main() {
    val port = System.getProperty("port", "7070").toInt()
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSockets(
        KanonenState.runner,
        KanonenState.pinConfig.outputs.values,
        KanonenState.pinConfig.inputs.values
    )
    configureSerialization(KanonenState)
    configureRouting()
}
