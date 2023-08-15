package se.smasseman.kanonen.core

import org.junit.Test
import java.lang.IllegalArgumentException
import kotlin.test.assertTrue
import kotlin.test.fail

class OutputNameTest {

    @Test
    fun testValidNames() {
        OutputName("FOO")
        OutputName("FOO1")
        OutputName("FOO_1")
    }

    @Test
    fun testInValidNames() {
        fun t(name: String) {
            try {
                OutputName(name)
                fail("Illegal name $name accepted")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
        t(".")
        t("")
        t(" ")
    }
}