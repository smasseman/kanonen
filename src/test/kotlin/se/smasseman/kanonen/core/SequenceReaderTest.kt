package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.time.Duration


class SequenceReaderTest {

    @Test
    fun readSetOn() {
        val expected = SetAction(OutputName("DVAB"), OutputState.ON)
        assertAction(expected, "SET DVAB ON")
    }

    @Test
    fun readSetOff() {
        val expected = SetAction(OutputName("DVAB"), OutputState.OFF)
        assertAction(expected, "SET DVAB OFF")
    }

    @Test
    fun readWaitFor() {
        val expected = WaitForAction(InputName("MICRO1"), InputState.OFF, Duration.ofSeconds(1))
        assertAction(expected, "WAITFOR MICRO1 OFF 1000")
    }

    @Test
    fun readLabel() {
        val expected = LabelAction("TheLabel")
        assertAction(expected, "LABEL TheLabel")
    }

    @Test
    fun readGoto() {
        val expected = GotoAction(SequenceName("S1"), "TheLabel")
        assertAction(expected, "GOTO S1 TheLabel")
    }

    @Test
    fun read() {
        val expected = GotoAction(SequenceName("S1"), "TheLabel")
        assertAction(expected, "GOTO S1 TheLabel")
    }

    @Test
    fun `read multiple lines`() {
        val sequence: Sequence = read(
            """
            SET DVAB ON
            SET DVAB OFF
        """.trimIndent()
        )
        assertThat(sequence.lines).hasSize(2)
    }

    private fun assertAction(expected: Action, code: String) {
        val read = read(code).lines.get(0).action
        assertThat(read).isEqualTo(expected)
    }

    private fun read(code: String) = SequenceReader.read(SequenceName("THE_SEQ_NAME"), code)
}