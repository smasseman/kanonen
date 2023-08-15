package se.smasseman.kanonen.core

import org.junit.Assert
import org.junit.Test
import java.lang.IllegalArgumentException

class SequenceNameTest {

    @Test
    fun testValidNames() {
        arrayOf("FOO").forEach { name -> SequenceName(name) }
    }

    @Test
    fun testInvalidNames() {
        arrayOf("f b").forEach { name ->
            try {
                SequenceName(name)
                Assert.fail("'$name' is not a valid name.")
            } catch (e: IllegalArgumentException) {
                // This was expected.
            }
        }
    }
}