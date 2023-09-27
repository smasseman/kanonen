package se.smasseman.kanonen.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.time.Duration
import java.util.*


class SequenceReaderTest {

    @Test
    fun readWrite() {
        val directory : File = File.createTempFile("foo", "bar").let {
            it.delete()
            File(it.parent, "SequenceReaderTest" + UUID.randomUUID())
        }
        directory.mkdir()

        val reader = SequenceReader(directory)

        val name = SequenceName("TEST_SEQ")
        reader.create(name)
        val code = """
            TRIGGER G1 ON
            ---
            SET OUT1 ON
        """.trimIndent()
        reader.update(name, code)
        val list = reader.readDirectory()
        assertThat(list).hasSize(1)
        val sequence = list[0]
        assertThat(sequence.lines).`as`(sequence.toString()).hasSize(3)

        assertThat(sequence.lines.map { it.raw }.joinToString(separator = "\n")).isEqualTo(code)
    }

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
        assertThat(sequence.actionLines).hasSize(2)
    }

    @Test
    fun `read sequense with trigger`() {
        val sequence: Sequence = read(
            """
            TRIGGER G3 ON
            ---
            SET DVAB ON
            SET DVAB OFF
        """.trimIndent()
        )
        assertThat(sequence.actionLines).hasSize(2)
        assertThat(sequence.propertyLines).hasSize(1)
        val property = sequence.propertyLines[0].property
        val triggerProperty = property as TriggerProperty
        assertThat(triggerProperty.input.name).isEqualTo("G3");

    }

    @Test
    fun `read sequense with abort`() {
        val sequence: Sequence = read(
            """
            ABORT G3 ON
            ---
            SET DVAB ON
            SET DVAB OFF
        """.trimIndent()
        )
        assertThat(sequence.actionLines).hasSize(2)
        assertThat(sequence.propertyLines).hasSize(1)
        val property = sequence.propertyLines[0].property
        val triggerProperty = property as AbortProperty
        assertThat(triggerProperty.input.name).isEqualTo("G3");

    }

    private fun assertAction(expected: Action, code: String) {
        val read = read(code).actionLines.get(0).action
        assertThat(read).isEqualTo(expected)
    }

    private fun read(code: String) = SequenceReader.read(SequenceName("THE_SEQ_NAME"), code)
}