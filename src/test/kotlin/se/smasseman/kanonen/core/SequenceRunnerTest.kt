package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.Test

class SequenceRunnerTest {

    private val output1 = MockOutput(OutputName("OUT1"))
    private val output2 = MockOutput(OutputName("OUT2"))
    private val outputs = listOf(output1, output2).associateBy { it.name }
    private val input1 = MockInput(InputName("IN1"))
    private val inputs = listOf(input1).associateBy { it.name }
    private val sequenceName1 = SequenceName("S1")
    private val sequenceName2 = SequenceName("S2")

    @Test
    fun testSetOutput() {
        output1.set(OutputState.OFF)
        val seq = SequenceReader(sequenceName1).read(
            """
            SET OUT1 ON
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.ON)
    }

    @Test
    fun testGoto() {
        output1.set(OutputState.OFF)
        output2.set(OutputState.OFF)
        val seq = SequenceReader(sequenceName1).read(
            """
            GOTO S1 A
            SET OUT1 ON
            LABEL A
            SET OUT2 ON
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.OFF)
        assertThat(output2.get()).isEqualTo(OutputState.ON)
    }

    @Test
    fun testCall() {
        output1.set(OutputState.OFF)
        output2.set(OutputState.OFF)
        val seq1 = SequenceReader(sequenceName1).read(
            """
            CALL S2 START
            SET OUT2 ON
        """.trimIndent()
        )
        val seq2 = SequenceReader(sequenceName2).read(
            """
            LABEL START
            SET OUT1 ON
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq1, seq2))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.ON)
        assertThat(output2.get()).isEqualTo(OutputState.ON)
    }

    @Test
    fun testIf_Output_Goto_thenTrue() {
        output1.set(OutputState.OFF)
        output2.set(OutputState.OFF)
        val seq = SequenceReader(sequenceName1).read(
            """
            IF OUTPUT OUT1 OFF GOTO S1 A
            SET OUT1 ON
            GOTO S1 END
            LABEL A
            SET OUT2 ON
            LABEL END
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.OFF)
        assertThat(output2.get()).isEqualTo(OutputState.ON)
    }

    @Test
    fun testIf_Output_Goto_whenFalse() {
        output1.set(OutputState.OFF)
        output2.set(OutputState.OFF)
        val seq = SequenceReader(sequenceName1).read(
            """
            IF OUTPUT OUT1 ON GOTO S1 A
            SET OUT1 ON
            GOTO S1 END
            LABEL A
            SET OUT2 ON
            LABEL END
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.OFF)
        assertThat(output2.get()).isEqualTo(OutputState.OFF)
    }

    @Test
    fun testIf_Input_Set_Output() {
        output1.set(OutputState.OFF)
        input1.set(InputState.ON)
        val seq = SequenceReader(sequenceName1).read(
            """
            IF INPUT IN1 ON SET OUT1 ON
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.ON)
    }

    @Test
    fun testIf_Input_Goto_whenFalse() {
        output1.set(OutputState.OFF)
        input1.set(InputState.OFF)
        val seq = SequenceReader(sequenceName1).read(
            """
            IF INPUT IN1 ON GOTO S1 A
            SET OUT1 ON
            GOTO S1 END
            LABEL A
            SET OUT1 ON
            LABEL END
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.OFF)
    }

    @Test
    fun testIf_Input_Goto_whenTrue() {
        output1.set(OutputState.OFF)
        input1.set(InputState.ON)
        val seq = SequenceReader(sequenceName1).read(
            """
            IF INPUT IN1 ON GOTO S1 A
            SET OUT1 ON
            GOTO S1 END
            LABEL A
            SET OUT1 ON
            LABEL END
        """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        unit.run(sequenceName1)
        assertThat(output1.get()).isEqualTo(OutputState.ON)
    }

    @Test
    fun testOnlyOneRunning() {
        output1.set(OutputState.ON)
        val seq = SequenceReader(sequenceName1).read(
            """
            WAIT 500
         """.trimIndent()
        )
        val sequences = SequenceProvider.from(listOf(seq))
        val unit = SequenceRunner(outputs, inputs, sequences)
        var counter = 0
        unit.addDoneListener { counter++ }
        Thread { unit.run(sequenceName1) }.start()
        assertThatCode { unit.run(sequenceName1) }.isInstanceOf(IllegalStateException().javaClass)
        Thread.sleep(1000)
        assertThat(counter).isEqualTo(1)

    }
}