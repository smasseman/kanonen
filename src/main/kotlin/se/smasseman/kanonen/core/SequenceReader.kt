package se.smasseman.kanonen.core

import java.time.Duration
import java.util.*

class SequenceReader(private val name: SequenceName) : LogUtil {

    fun read(input: String): Sequence {
        logger().info("Read sequence $name")
        val scanner = Scanner(input)

        val lines = LinkedList<SequenceLine>()
        var actionFlag = !input.contains("---")
        var lineNumber = 0
        while (scanner.hasNext()) {
            lineNumber++
            val line = scanner.nextLine().trim()
            logger().debug("Parse line ${lineNumber}: $line")
            try {
                val sequenceLine: SequenceLine = when {
                    (line.startsWith("#")) -> SequenceNoopLine(name, lineNumber, line)
                    (line.isBlank()) -> SequenceNoopLine(name, lineNumber, line)
                    (line.contains("---")) -> {
                        actionFlag = true
                        SequenceNoopLine(name, lineNumber, line)
                    }

                    actionFlag -> SequenceActionLine(name, lineNumber, parseAction(line), line)
                    else -> SequencePropertyLine(name, lineNumber, parseProperty(line), line)
                }
                logger().debug("SequenceLine: {}", sequenceLine)
                lines.add(sequenceLine)
            } catch (e: Exception) {
                throw SequenceSyntaxErrorException("Error in sequence $name at line $lineNumber: '$line' Error message: ${e.message}")
                    .initCause(e)
            }
        }
        return Sequence(name, lines)
    }

    private fun parseProperty(line: String): Property {
        return if (line.startsWith("TRIGGER")) {
            //TRIGGER G3 OFF
            val scanner = Scanner(line).useDelimiter(" ")
            scanner.next()
            TriggerProperty(InputName(scanner.next()), parseInputState(scanner, line))
        } else if (line.startsWith("ABORT")) {
            //ABORT G3 OFF
            val scanner = Scanner(line).useDelimiter(" ")
            scanner.next()
            AbortProperty(InputName(scanner.next()), parseInputState(scanner, line))
        } else {
            throw SequenceSyntaxErrorException("Invalid property $line")
        }
    }

    private fun parseAction(line: String): Action {
        val scanner = Scanner(line).useDelimiter(" ")
        return parseAction(scanner, line)
    }

    private fun parseAction(scanner: Scanner, line: String): Action {
        return when (scanner.next()) {
            "SET" -> parseSetAction(scanner, line)
            "WAITFOR" -> parseWaitForAction(scanner, line)
            "EXPECT" -> parseExpectAction(scanner, line)
            "IF" -> parseIf(scanner, line)
            "GOTO" -> parseGotoAction(scanner)
            "CALL" -> parseCallAction(scanner)
            "WAIT" -> parseWaitAction(scanner)
            "LABEL" -> parseLabelAction(scanner)
            else -> throw SequenceSyntaxErrorException("Unexpected start of line: $line")
        }
    }

    private fun parseIf(scanner: Scanner, line: String): Action {
        return when (scanner.next()) {
            "INPUT" -> parseIfInputAction(scanner, line)
            "OUTPUT" -> parseIfOutputAction(scanner, line)
            else -> throw SequenceSyntaxErrorException("Unexpected start of line: $line")
        }
    }

    private fun parseIfInputAction(scanner: Scanner, line: String): IfInputAction {
        val inputName = InputName(scanner.next())
        val state = parseInputState(scanner, line)
        val action = parseAction(scanner, line)
        return IfInputAction(inputName, state, action)
    }

    private fun parseIfOutputAction(scanner: Scanner, line: String): IfOutputAction {
        val outputName = OutputName(scanner.next())
        val state = parseOutputState(scanner, line)
        val action = parseAction(scanner, line)
        return IfOutputAction(outputName, state, action)
    }

    private fun parseSetAction(scanner: Scanner, line: String) = SetAction(
        OutputName(scanner.next()), parseOutputState(scanner, line)
    )

    private fun parseWaitForAction(scanner: Scanner, line: String): WaitForAction {
        val name = InputName(scanner.next())
        val value = parseInputState(scanner, line)
        val duration = Duration.ofMillis(scanner.nextLong())
        return WaitForAction(name, value, duration)
    }

    private fun parseLabelAction(scanner: Scanner) = LabelAction(scanner.next())

    private fun parseWaitAction(scanner: Scanner) = WaitAction(Duration.ofMillis(scanner.next().toLong()))

    private fun parseCallAction(scanner: Scanner) = CallAction(SequenceName(scanner.next()), scanner.next())

    private fun parseGotoAction(scanner: Scanner): GotoAction {
        if (!scanner.hasNext()) throw SequenceSyntaxErrorException("Must have a label name after GOTO (and optional sequence name), eg. GOTO SEQ1 MyLabel")

        val string = scanner.next()
        return if (scanner.hasNext()) {
            GotoAction(SequenceName(string), scanner.next())
        } else {
            GotoAction(this.name, string)
        }

    }

    private fun parseExpectAction(scanner: Scanner, line: String) = ExpectAction(
        OutputName(scanner.next()), parseOutputState(scanner, line)
    )

    private fun parseOutputState(scanner: Scanner, line: String) = when (scanner.next()) {
        "ON" -> OutputState.ON
        "OFF" -> OutputState.OFF
        else -> throw SequenceSyntaxErrorException("Invalid ON/OFF in: $line")
    }

    private fun parseInputState(scanner: Scanner, line: String) = when (scanner.next()) {
        "ON" -> InputState.ON
        "OFF" -> InputState.OFF
        else -> throw SequenceSyntaxErrorException("Invalid ON/OFF in: $line")
    }
}