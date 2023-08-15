package se.smasseman.kanonen.web.plugins

import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.databind.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import se.smasseman.kanonen.web.KanonenState
import se.smasseman.kanonen.core.*

fun Application.configureSerialization(KanonenState: KanonenState) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    routing {
        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/run/{name}") {
            KanonenState.runSequence(SequenceName(call.parameters["name"].toString()))
            call.respondText(text = "ok")
        }
        get("/sequencenames") {
            call.respond(KanonenState.sequences.map { it.name.name })
        }
        get("/validate/sequences") {
            try {
                KanonenState.validateSequences()
                call.respond("ok")
            } catch (e: ValidationFailedException) {
                call.respondText(text = e.message, status = HttpStatusCode.Conflict)
            }
        }
        get("/sequence/{name}") {
            val sequence = KanonenState.sequences.filter { it.name.name == call.parameters["name"] }
                .firstOrNull()
            if (sequence == null) {
                call.respondText(text = "", status = HttpStatusCode.NoContent)
            } else {
                call.respond(SequenceDto.toDto(sequence))
            }
        }
        put("/sequence/{name}") {
            val body = call.receive(SequenceUpdate::class);
            val sequenceName = SequenceName(call.parameters["name"].toString());
            try {
                KanonenState.updateSequence(sequenceName, body.code)
                call.respond("{}")
            } catch (e: SequenceSyntaxErrorException) {
                LoggerFactory.getLogger(this::class.java).warn(e.message, e)
                call.respondText(text = e.message, status = HttpStatusCode.BadRequest)
            }
        }
        delete("/sequence/{name}") {
            val sequenceName = SequenceName(call.parameters["name"].toString());
            try {
                KanonenState.deleteSequence(sequenceName)
                call.respond("{}")
            } catch (e: SequenceSyntaxErrorException) {
                LoggerFactory.getLogger(this::class.java).warn(e.message, e)
                call.respondText(text = e.message, status = HttpStatusCode.BadRequest)
            }
        }
        post("/sequence/{name}") {
            val sequenceName = SequenceName(call.parameters["name"].toString());
            KanonenState.newSequence(sequenceName)
            call.respond("{}")
        }
    }
}

data class SequenceUpdate(val code: String)

data class SequenceDto(val name: String, val lines: List<LineDto>) {
    companion object {
        fun toDto(sequence: Sequence): SequenceDto = SequenceDto(
            sequence.name.name,
            sequence.lines.map { LineDto.toDto(it) }.toList()
        )
    }
}

data class LineDto(val lineNumber: Int, val raw: String) {
    companion object {
        fun toDto(line: SequenceLine): LineDto = LineDto(
            line.lineNumber,
            line.raw
        )
    }
}
