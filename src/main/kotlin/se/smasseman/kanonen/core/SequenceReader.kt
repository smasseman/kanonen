package se.smasseman.kanonen.core

import java.io.File
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.*

class SequenceReader(private val directory: File) : LogUtil {

    init {
        if (!directory.isDirectory) {
            throw IllegalArgumentException("$directory is not a directory")
        }
    }

    fun readDirectory(): List<Sequence> {
        return readDirectory(directory)
    }

    fun readDirectory(directory: File): List<Sequence> {
        logger().info("Read sequences in $directory")
        return (directory.listFiles()
            ?: throw RuntimeException("Could not list files in directory $directory"))
            .filter { it.isFile }
            .filter { it.name.endsWith(".seq") }
            .map {
                read(
                    SequenceName(it.name.substringBeforeLast(".")),
                    Files.readString(it.toPath())
                )
            }
            .toList()
    }

    companion object : LogUtil {

        fun read(name: SequenceName, input: String): Sequence {
            logger().info("Load sequence $name")
            val scanner = Scanner(input)

            val sequenceLines = LinkedList<SequenceLine>()
            var actionFlag = false
            var lineNumber = 0
            while (scanner.hasNext()) {
                lineNumber++
                val line = scanner.nextLine().trim()
                logger().debug("Parse line ${lineNumber}: $line")
                try {
                    if (line.startsWith("#")) {
                        // Ignore comment
                    } else if (line.isBlank()) {
                        // Ignore blank line
                    } else {
                        if (!line.contains(":")) actionFlag = true;
                        if (actionFlag) {
                            val action = parseAction(line)
                            val sequenceLine = SequenceLine(name, lineNumber, action, line)
                            logger().debug("SequenceLine: $sequenceLine")
                            sequenceLines.add(sequenceLine)
                        }
                    }
                } catch (e: Exception) {
                    throw SequenceSyntaxErrorException("Error in sequence $name at line $lineNumber: '$line' Error message: ${e.message}")
                        .initCause(e)
                }
            }
            return Sequence(name, mapOf(), sequenceLines)
        }

        fun parseAction(line: String): Action {
            val scanner = Scanner(line).useDelimiter(" ")
            return when (scanner.next()) {
                "SET" ->
                    SetAction(
                        OutputName(scanner.next()), when (scanner.next()) {
                            "ON" -> OutputState.ON
                            "OFF" -> OutputState.OFF
                            else -> throw RuntimeException("Invalid ON/OFF in: $line")
                        }
                    )

                "WAITFOR" -> {
                    val name = InputName(scanner.next())
                    val value = when (scanner.next()) {
                        "ON" -> InputState.ON
                        "OFF" -> InputState.OFF
                        else -> throw RuntimeException("Invalid ON/OFF in: $line");
                    }
                    val duration = Duration.ofMillis(scanner.nextLong())
                    return WaitForAction(name, value, duration)
                }

                "EXPECT" ->
                    ExpectAction(
                        OutputName(scanner.next()), when (scanner.next()) {
                            "ON" -> OutputState.ON
                            "OFF" -> OutputState.OFF
                            else -> throw RuntimeException("Invalid ON/OFF in: $line")
                        }
                    )

                "GOTO" -> GotoAction(SequenceName(scanner.next()), scanner.next())
                "CALL" -> CallAction(SequenceName(scanner.next()), scanner.next())
                "WAIT" -> WaitAction(Duration.ofMillis(scanner.next().toLong()))
                "LABEL" -> LabelAction(scanner.next())

                else -> throw RuntimeException("Unexpected start of line: $line")
            }
        }
    }

    fun update(sequenceName: SequenceName, code: String) {
        val file = file(sequenceName)
        logger().info("Update content in $file")
        logger().info("Code:\n$code")
        if (!file.exists()) {
            throw FileNotFoundException(file.toString())
        }
        read(sequenceName, code)
        Files.write(file.toPath(), code.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun delete(sequenceName: SequenceName) {
        val file = file(sequenceName)
        logger().info("Delete $file")
        if (!file.delete()) {
            throw Exception("Failed to delete $file")
        }
    }

    fun create(sequenceName: SequenceName) {
        val file = file(sequenceName)
        logger().info("Create $file")
        if (!file.createNewFile()) {
            throw Exception("Failed to create $file")
        }
    }

    private fun file(sequenceName: SequenceName): File =
        File(directory, "$sequenceName.seq").absoluteFile
}
