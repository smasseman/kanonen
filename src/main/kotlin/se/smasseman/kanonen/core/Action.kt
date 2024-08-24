package se.smasseman.kanonen.core

import java.time.Duration

sealed class Action

data class SetAction(val output: OutputName, val to: OutputState) : Action()
data class ExpectAction(val output: OutputName, val expected: OutputState) : Action()
data class LabelAction(val labelName: String) : Action()
data class WaitAction(val duration: Duration) : Action()
data class WaitForAction(val inputName: InputName, val value: InputState, val duration: Duration) : Action()
sealed class JumpAction(val sequenceName: SequenceName, val labelName: String) : Action()
data class IfInputAction(val input: InputName, val state: InputState, val action: Action) : Action()
data class IfOutputAction(val output: OutputName, val state: OutputState, val action: Action) : Action()
data class GotoAction(val sequence: SequenceName, val label: String) : JumpAction(sequence, label)
data class CallAction(private val s: SequenceName, private val l: String) : JumpAction(s, l)
