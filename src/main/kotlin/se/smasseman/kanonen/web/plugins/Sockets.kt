package se.smasseman.kanonen.web.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext
import org.slf4j.LoggerFactory
import se.smasseman.kanonen.core.*
import java.time.Duration
import java.util.*
import kotlin.collections.HashMap

fun Application.configureSockets(
    runner: SequenceRunner,
    outputs: Collection<Output>,
    inputs: Collection<Input>
) {

    val ctx = newFixedThreadPoolContext(1, "socketContext")
    val log = LoggerFactory.getLogger(this::class.java)
    val connections = Collections.synchronizedSet<DefaultWebSocketSession?>(LinkedHashSet())
    val send = fun(event: WebSocketEvent) {
        connections.forEach {
            LoggerFactory.getLogger(this::class.java).info("Send $event")
            async(context = ctx) { it.send(event.toJson()) }
        }
    }
    inputs.forEach {
        it.addListener(fun(n: InputName, s: InputState) {
            send(InputStateEvent(n.name, s.name))
        })
    }
    runner
        .addDoneListener { send(DoneEvent()) }
        .addErrorListener { error -> send(ExecutionErrorEvent.from(error)) }
        .addOutputListener { outputName, outputState ->
            send(OutputStateEvent(outputName.name, outputState.toString()))
        }
        .addExecutionListener { line -> send(ExecutionEvent.from(line)) }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") { // websocketSession
            log.info("New connection.")
            val x = HashMap<String, String>()
            outputs.forEach { x[it.name().name] = it.get().toString() }
            val inputMap = HashMap<String, String>()
            inputs.forEach { inputMap[it.name.name] = it.state.name }
            send(ConnectedEvent(x, inputMap).toJson())
            connections.add(this)
            this.closeReason.await().run {
                log.info("Closed connection")
                connections.remove(this@webSocket)
            }
        }
    }
}

enum class WebSocketEventType {
    SEQUENCE_DONE,
    CONNECTED,
    ERROR,
    OUTPUT_STATE,
    INPUT_STATE,
    EXECUTE
}

abstract class WebSocketEvent(val type: WebSocketEventType) {
    fun toJson(): String {
        return ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this)
    }
}

class DoneEvent() : WebSocketEvent(WebSocketEventType.SEQUENCE_DONE)
class ConnectedEvent(val outputs: Map<String, String>, val inputs: Map<String, String>) :
    WebSocketEvent(WebSocketEventType.CONNECTED)

class OutputStateEvent(val outputName: String, val state: String) :
    WebSocketEvent(WebSocketEventType.OUTPUT_STATE) {
    override fun toString() = this.javaClass.simpleName + "[" + outputName + "=" + state + "]"
}

class InputStateEvent(val inputName: String, val state: String) :
    WebSocketEvent(WebSocketEventType.INPUT_STATE) {
    override fun toString() = this.javaClass.simpleName + "[" + inputName + "=" + state + "]"
}

class ExecutionEvent(val raw: String) : WebSocketEvent(WebSocketEventType.EXECUTE) {
    companion object {
        fun from(line: SequenceLine): ExecutionEvent = ExecutionEvent(line.raw)
    }

    override fun toString() = this.javaClass.simpleName + "[" + raw + "]"
}

class ExecutionErrorEvent(val message: String) : WebSocketEvent(WebSocketEventType.ERROR) {
    companion object {
        fun from(error: ExecutionError): ExecutionErrorEvent =
            ExecutionErrorEvent(error.message)
    }

    override fun toString() = this.javaClass.simpleName + "[" + message + "]"
}