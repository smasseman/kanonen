package se.smasseman.kanonen.core

import org.slf4j.LoggerFactory
import java.util.*

class SequenceRunner(
    private val outputs: Map<OutputName, Output>,
    private val inputs: Map<InputName, Input>,
    private val sequences: SequenceProvider
) :
    LogUtil {

    private val outputListeners = LinkedList<OutputStateListener>()
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val errorListeners = LinkedList<ExecutionErrorListener>()
    private val executionListeners = LinkedList<ExecutionListener>()
    private val doneListeners = LinkedList<DoneListener>()
    private var runningThread: Thread? = null

    fun run(sequenceName: SequenceName) {
        if (runningThread != null) {
            throw IllegalStateException("A sequence is already running.")
        }
        runningThread = Thread.currentThread()
        try {
            run(sequences.get(sequenceName).actionLines[0])
        } finally {
            runningThread = null
        }
    }

    private fun run(input: SequenceActionLine) {
        val sequenceName = input.sequenceName
        val sequence = sequences.get(sequenceName)
        val iter = getIterator(input, sequence)
        while (iter.hasNext() && !Thread.interrupted()) {
            val line = iter.next()
            val action = line.action
            log.debug("In sequence: {} Run: {}", sequence.name, action)
            executionListeners.forEach { it.execute(line) }
            try {
                if (executeAction(action)) return@run
            } catch (e: InterruptedException) {
                log.info("Interrupted")
                Thread.currentThread().interrupt()
            } catch (e: RuntimeException) {
                val error = ExecutionError(line, e.message ?: "")
                logger().warn(error.toString(), e)
                errorListeners.forEach { it.error(error) }
                Thread.currentThread().interrupt()
            }
        }
        doneListeners.forEach { it.done() }
    }

    private fun executeAction(action: Action): Boolean {
        when (action) {
            is SetAction -> executeSet(action)
            is ExpectAction -> executeExpect(action)
            is WaitForAction -> executeWaitFor(action)
            is CallAction -> {
                run(action.sequenceName, action.labelName, action)
            }

            is GotoAction -> {
                run(action.sequenceName, action.labelName, action)
                return true
            }

            is IfInputAction -> {
                executeIfInput(action)
                if (action.action is GotoAction) {
                    return true
                }
            }

            is IfOutputAction -> {
                executeIfOutput(action)
                if (action.action is GotoAction) {
                    return true
                }
            }

            is LabelAction -> {}
            is WaitAction -> executeWait(action)
        }
        return false
    }

    private fun executeWaitFor(action: WaitForAction) {
        val input = inputs[action.inputName]
            ?: throw Exception("There is no input with name ${action.inputName}")
        val maxTime = System.currentTimeMillis() + action.duration.toMillis()
        val isOverdue = fun(): Boolean {
            return System.currentTimeMillis() > maxTime
        }
        while (!isOverdue()) {
            if (input.state == action.value) {
                return;
            } else {
                Thread.sleep(10)
            }
        }
        throw RuntimeException("Input ${action.inputName} is still not ${action.value} after ${action.duration.toMillis()} milliseconds.")
    }

    private fun getIterator(input: SequenceActionLine, sequence: Sequence): Iterator<SequenceActionLine> {
        val iter1 = sequence.actionLines.iterator()
        val iter2 = sequence.actionLines.iterator()
        while (iter1.next() != input) {
            iter2.next()
        }
        return iter2;
    }

    private fun executeWait(action: WaitAction) = Thread.sleep(action.duration.toMillis())

    private fun run(sequenceName: SequenceName, labelName: String, action: Action) {
        val sequence = sequences.get(sequenceName)
        val line =
            sequence.actionLines.find { it.action is LabelAction && it.action.labelName == labelName }
                ?: throw RuntimeException("Can not $action because there is no such label in $sequenceName")
        run(line)
    }

    private fun executeExpect(action: ExpectAction) {
        val outputName = action.output
        val expectedValue = action.expected
        val currentValue = outputs[outputName]!!.get()
        if (currentValue != expectedValue) {
            throw java.lang.RuntimeException("$outputName was expected to be $expectedValue but it was $currentValue")
        }
    }

    private fun executeSet(action: SetAction) {
        val outputName = action.output
        val value = action.to
        validate(outputName)
        outputs[outputName]!!.set(value)
        outputListeners.forEach { it.changed(outputName, value) }
    }

    private fun executeIfInput(action: IfInputAction) {
        val inputName = action.input
        val expectedValue = action.state
        val currentValue = inputs[inputName]!!.state
        if (currentValue == expectedValue) {
            executeAction(action.action)
        }
    }

    private fun executeIfOutput(action: IfOutputAction) {
        val outputName = action.output
        val expectedValue = action.state
        val currentValue = outputs[outputName]!!.get()
        if (currentValue == expectedValue) {
            executeAction(action.action)
        }
    }

    private fun validate(output: OutputName) {
        if (!outputs.keys.contains(output)) {
            throw RuntimeException("Output $output is not known.")
        }
    }

    fun addErrorListener(f: ExecutionErrorListener): SequenceRunner {
        errorListeners.add(f);
        return this
    }

    fun addExecutionListener(f: ExecutionListener): SequenceRunner {
        executionListeners.add(f);
        return this
    }

    fun addDoneListener(f: DoneListener): SequenceRunner {
        doneListeners.add(f);
        return this
    }

    fun addOutputListener(listener: OutputStateListener): SequenceRunner {
        outputListeners.add(listener)
        return this
    }

}