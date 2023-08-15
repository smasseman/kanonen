package se.smasseman.kanonen.core

import org.slf4j.LoggerFactory
import java.awt.Label
import java.util.LinkedList

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

    fun run(sequenceName: SequenceName) {
        run(sequences.get(sequenceName).lines[0])
    }

    private fun run(input: SequenceLine) {
        val sequenceName = input.sequenceName
        val sequence = sequences.get(sequenceName)
        val iter = getIterator(input, sequence)
        log.info("Going to run ${sequence.lines.size} lines in ${sequence.name}")
        while (iter.hasNext() && !Thread.interrupted()) {
            val line = iter.next()
            val action = line.action
            println("Execute $sequenceName ${line.lineNumber} $action")
            try {
                executionListeners.forEach { it.execute(line) }
                when (action) {
                    is SetAction -> executeSet(action)
                    is ExpectAction -> executeExpect(action)
                    is WaitForAction -> executeWaitFor(action)
                    is GotoAction -> {
                        run(action.sequenceName, action.labelName, action)
                        return@run
                    }

                    is CallAction -> run(action.sequenceName, action.labelName, action)
                    is LabelAction -> {}
                    is WaitAction -> executeWait(action)
                }
            } catch (e: InterruptedException) {
                println("Interrupted")
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

    private fun getIterator(input: SequenceLine, sequence: Sequence): Iterator<SequenceLine> {
        val iter1 = sequence.lines.iterator()
        val iter2 = sequence.lines.iterator()
        while (iter1.next() != input) {
            iter2.next()
        }
        return iter2;
    }

    private fun executeWait(action: WaitAction) = Thread.sleep(action.duration.toMillis())

    private fun run(sequenceName: SequenceName, labelName: String, action: Action) {
        val sequence = sequences.get(sequenceName)
        val line =
            sequence.lines.find { it.action is LabelAction && it.action.labelName == labelName }
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