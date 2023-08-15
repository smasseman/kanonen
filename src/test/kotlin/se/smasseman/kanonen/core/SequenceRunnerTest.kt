package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class SequenceRunnerTest {

    private val output1 = MockOutput(OutputName("OUT1"))
    private val outputs = listOf(output1).associateBy { it.name }
    private val input1 = MockInput(InputName("IN1"))
    private val inputs = listOf(input1).associateBy { it.name }
    private val sequenceName1 = SequenceName("S1")

    @Test
    fun testSetOutput() {
        output1.set(OutputState.OFF)
        val seq = SequenceReader.read(
            sequenceName1, """
            SET OUT1 ON
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.ON)
    }

    //@Test(timeout = 1000)
    fun testOnlyOneRunning() {
        output1.set(OutputState.ON)
        val seq = SequenceReader.read(
            sequenceName1, """
            LABEL START
            WAIT 500
            SET ${output1.name} OFF
            GOTO S1 START
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        while( output1.get() == OutputState.ON) {
            Thread.sleep(10);
        }
    }
}