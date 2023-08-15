package se.smasseman.kanonen.core

interface Output {

    fun name() : OutputName
    fun set(state: OutputState)
    fun get() : AnyOutputState

}
