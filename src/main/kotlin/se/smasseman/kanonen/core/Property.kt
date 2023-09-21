package se.smasseman.kanonen.core

sealed class Property

data class TriggerProperty(val input: InputName, val inputState: InputState) : Property()
