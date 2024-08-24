package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.RuntimeException
import java.time.Duration
import java.time.LocalDate
import java.util.LinkedList
import java.util.function.Function

class KanonenIT {

    private val errorListener = MyExecutionErrorListener()
    private val executionListener = MyExecutionListener()
    private val doneListener = MyDoneListener()
    private val outputListener = MyOutputListener()
    private val outputs: Map<OutputName, MockOutput> = getOutputs("OUTA", "OUTB")
    private val inputs: Map<InputName, MockInput> = getInputs("MICRO1", "MICRO2")

    @Test
    fun test() {
        val s1: Sequence = readSequence(
            "SE1", """
        SET OUTA ON
        SET OUTA OFF
        EXPECT OUTA OFF
        """
        )
        val sequences = listOf(s1)
        val runner = getRunner(sequences)

        runner.run(s1.name)

        doneListener.await(duration = Duration.ofSeconds(5))
        assertThat(outputListener.callsByOutput).hasSize(1)
        assertThat(executionListener.lines).hasSize(3)
        assertThat(errorListener.errors).isEmpty()
    }

    @Test
    fun testFailing_WaitFor() {
        val input: MockInput = inputs[InputName("MICRO1")]!!
        input.set(InputState.OFF)
        val s1: Sequence = readSequence(
            "SE1", """
        WAITFOR MICRO1 ON 1
        """
        )
        val sequences = listOf(s1)
        val runner = getRunner(sequences)

        runner.run(s1.name)

        doneListener.await(duration = Duration.ofSeconds(5))
        assertThat(executionListener.lines).hasSize(1)
        assertThat(errorListener.errors).hasSize(1)
    }

    @Test
    fun testSuccessful_WaitFor() {
        val mockInput = inputs[InputName("MICRO1")]!!
        mockInput.set(InputState.OFF)
        val s1: Sequence = readSequence(
            "SE1", """
        WAITFOR MICRO1 ON 1000
        """
        )
        val sequences = listOf(s1)
        val runner = getRunner(sequences)

        Thread(Runnable {
            Thread.sleep(500)
            mockInput.set(InputState.ON)
        }).start()

        runner.run(s1.name)

        doneListener.await(duration = Duration.ofSeconds(5))
        assertThat(executionListener.lines).hasSize(1)
        assertThat(errorListener.errors).isEmpty()
    }

    @Test
    fun testFailedExpect() {
        val s1: Sequence = readSequence(
            "SE1", """
        SET OUTA ON
        EXPECT OUTA OFF
        """
        )
        val sequences = listOf(s1)
        val runner = getRunner(sequences)

        runner.run(s1.name)

        doneListener.await(duration = Duration.ofSeconds(5))
        assertThat(executionListener.lines).hasSize(2)
        with(errorListener.errors) {
            assertThat(this).hasSize(1)
            with(this[0].line.action) {
                assertThat(this is ExpectAction).`as`(this.javaClass.toString()).isTrue
            }
        }
    }

    private fun getRunner(sequences: List<Sequence>): SequenceRunner {
        val provider = SequenceProvider.from(sequences)
        return SequenceRunner(outputs, inputs, provider)
            .addErrorListener(errorListener)
            .addExecutionListener(executionListener)
            .addDoneListener(doneListener)
            .addOutputListener(outputListener)
    }

    private fun readSequence(name: String, content: String): Sequence {
        return SequenceReader(SequenceName(name)).read(content)
    }

    private fun getOutputs(vararg name: String): Map<OutputName, MockOutput> =
        name.map { MockOutput(OutputName(it)) }
            .associateBy { it.name }

    private fun getInputs(vararg name: String): Map<InputName, MockInput> =
        name.map { MockInput(InputName(it)) }
            .associateBy { it.name }
}

class MyExecutionListener : ExecutionListener {

    val lines = LinkedList<SequenceActionLine>()

    override fun execute(line: SequenceActionLine) {
        lines.add(line)
    }
}

class MyDoneListener : DoneListener {

    private var doneCounter = 0
    private val lock = Object()

    override fun done() {
        doneCounter++
    }

    fun await(expected: Int = 1, duration: Duration) {
        val max = System.currentTimeMillis() + duration.toMillis()
        while (doneCounter < expected && System.currentTimeMillis() < max) {
            val ms = max - System.currentTimeMillis()
            if (ms > 0) lock.wait()
        }
        if (doneCounter < expected) throw RuntimeException("Done counter is still $doneCounter")
    }
}

class MyOutputListener : (OutputName, OutputState) -> Unit {
    val callsByOutput = HashMap<OutputName, LinkedList<OutputState>>()

    override fun invoke(name: OutputName, state: OutputState) {
        val f = Function<OutputName, LinkedList<OutputState>> { LinkedList<OutputState>() }
        callsByOutput.computeIfAbsent(name, f).add(state)
    }

}

class MyExecutionErrorListener : ExecutionErrorListener {

    val errors = LinkedList<ExecutionError>()
    val timestamps = LinkedList<LocalDate>()
    override fun error(error: ExecutionError) {
        errors.add(error)
        timestamps.add(LocalDate.now())
    }

}
