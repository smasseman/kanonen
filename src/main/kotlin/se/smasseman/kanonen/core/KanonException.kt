package se.smasseman.kanonen.core

open class KanonException(message: String) : Exception(message) {

    override val message: String
        get() = super.message!!
}
