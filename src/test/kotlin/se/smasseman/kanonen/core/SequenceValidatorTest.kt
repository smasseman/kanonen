package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class SequenceValidatorTest {

    @Test
    fun test_validTriggerSequence() {
        val seq = SequenceReader.read(
            SequenceName("SEQ1"), """
            TRIGGER IN1 ON
            ---
            SET OUT1 ON
        """.trimIndent()
        )
        SequenceValidator.validate(listOf(OutputName("OUT1")), listOf(InputName("IN1")), listOf(seq))
    }

    @Test
    fun `test it explodes when trigger refere to unknow input`() {
        val seq = SequenceReader.read(
            SequenceName("SEQ1"), """
            TRIGGER IN2 ON
            ---
            SET OUT1 ON
        """.trimIndent()
        )
        assertThatThrownBy {
            SequenceValidator.validate(
                listOf(OutputName("OUT1")),
                listOf(InputName("IN1")),
                listOf(seq)
            )
        }.hasMessageContaining("IN1")
    }
}