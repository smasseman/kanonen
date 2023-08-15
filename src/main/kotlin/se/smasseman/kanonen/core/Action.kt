package se.smasseman.kanonen.core

import java.time.Duration

sealed class Action

data class SetAction(val output: OutputName, val to: OutputState) : Action()
data class ExpectAction(val output: OutputName, val expected: OutputState) : Action()
data class LabelAction(val labelName: String) : Action()
data class WaitAction(val duration: Duration) : Action()
data class WaitForAction(val inputName: InputName, val value: InputState, val duration: Duration) : Action()
sealed class JumpAction(val sequenceName: SequenceName, val labelName: String) : Action()
data class GotoAction(private val s: SequenceName, private val l: String) : JumpAction(s, l)
data class CallAction(private val s: SequenceName, private val l: String) : JumpAction(s, l)
