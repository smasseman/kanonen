package se.smasseman.kanonen.core

import org.slf4j.LoggerFactory;
import java.util.LinkedList

class MockOutput(val name: OutputName) : Output {

    private val log = LoggerFactory.getLogger(this.javaClass)
    private var state : AnyOutputState = AnyOutputState.UNKNOWN

    override fun name(): OutputName {
        return name
    }

    override fun set(state: OutputState) {
        log.info("$name=$state")
        this.state = state
    }

    override fun get(): AnyOutputState = state

}
