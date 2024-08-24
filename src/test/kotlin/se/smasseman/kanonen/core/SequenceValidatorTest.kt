package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class SequenceValidatorTest {

    @Test
    fun test_validTriggerSequence() {
        val seq = SequenceReader(SequenceName("SEQ1")).read(
            """
            TRIGGER IN1 ON
            ---
            SET OUT1 ON
        """.trimIndent()
        )
        SequenceValidator(listOf(OutputName("OUT1")), listOf(InputName("IN1")), listOf(seq)).validate()
    }

    @Test
    fun `test it explodes when trigger refer to unknown input`() {
        val seq = SequenceReader(SequenceName("SEQ1")).read(
            """
            TRIGGER IN2 ON
            ---
            SET OUT1 ON
        """.trimIndent()
        )
        assertThatThrownBy {
            SequenceValidator(
                listOf(OutputName("OUT1")),
                listOf(InputName("IN1")),
                listOf(seq)
            ).validate()
        }
            .hasMessageContaining("IN1")
            .hasMessageContaining("IN2")
    }

    @Test
    fun `test when sequence have 2 labels with same name then it explodes`() {
        val seq = SequenceReader(SequenceName("SEQ1")).read(
            """
            LABEL XXX
            LABEL FOO
            LABEL BAR
            LABEL FOO
        """.trimIndent()
        )
        assertThatThrownBy {
            SequenceValidator(
                listOf(),
                listOf(),
                listOf(seq)
            ).validate()
        }
            .hasMessageContaining("SEQ1")
            .hasMessageContaining("FOO")
    }
}