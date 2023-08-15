package se.smasseman.kanonen.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.newFixedThreadPoolContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import se.smasseman.kanonen.web.KanonenState
import java.io.File
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

    @Test
    fun testGoto() {
        val s1: Sequence = readSequence(
            "S1", """
        SET OUTA ON
        GOTO S2 label1
        EXPECT OUTA OFF
        """
        )
        val s2: Sequence = readSequence(
            "S2", """
        EXPECT OUTA OFF
        LABEL label1
        EXPECT OUTA ON
        """
        )
        val sequences = listOf(s1, s2)
        val runner = getRunner(sequences)

        runner.run(s1.name)

        doneListener.await(duration = Duration.ofSeconds(5))
        assertNoErrors()
        assertExecutedActions(
            "SET OUTA ON",
            "GOTO S2 label1",
            "LABEL label1",
            "EXPECT OUTA ON"
        );

    }

    @Test
    fun testCall() {
        val s1: Sequence = readSequence(
            "S1", """
        SET OUTA ON
        CALL S2 label1
        EXPECT OUTB ON
        """.trimIndent()
        )
        val s2: Sequence = readSequence(
            "S2", """
        LABEL label1
        CALL S3 label1
        SET OUTB ON
        """.trimIndent()
        )
        val s3: Sequence = readSequence(
            "S3", """
        LABEL label1
        SET OUTA OFF
        """.trimIndent()
        )
        val sequences = listOf(s1, s2, s3)
        SequenceValidator.validate(outputs.keys, inputs.keys, sequences);
        val runner = getRunner(sequences)

        runner.run(s1.name)

        doneListener.await(duration = Duration.ofSeconds(5))
        assertNoErrors()
        assertExecutedActions(
            "SET OUTA ON",
            "CALL S2 label1",
            "LABEL label1",
            "CALL S3 label1",
            "LABEL label1",
            "SET OUTA OFF",
            "SET OUTB ON",
            "EXPECT OUTB ON"
        );
    }

    private fun assertExecutedActions(vararg actions: String) {
        val expectedActions: List<Action> = actions.map { SequenceReader.parseAction(it) }
        assertThat(executionListener.lines.map { it.action }.toList()).containsExactlyElementsOf(
            expectedActions
        )
    }

    private fun assertNoErrors() {
        assertThat(errorListener.errors).`as`("Unexpected error: ${errorListener.errors}").isEmpty()
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
        return SequenceReader.read(SequenceName(name), content)
    }

    private fun getOutputs(vararg name: String): Map<OutputName, MockOutput> =
        name.map { MockOutput(OutputName(it)) }
            .associateBy { it.name }

    private fun getInputs(vararg name: String): Map<InputName, MockInput> =
        name.map { MockInput(InputName(it)) }
            .associateBy { it.name }
}

class MyExecutionListener : ExecutionListener {

    val lines = LinkedList<SequenceLine>()

    override fun execute(line: SequenceLine) {
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
