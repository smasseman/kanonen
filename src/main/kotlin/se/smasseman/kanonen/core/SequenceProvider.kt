package se.smasseman.kanonen.core

import se.smasseman.kanonen.web.KanonenState

interface SequenceProvider {

    fun get(): List<Sequence>

    fun get(name: SequenceName): Sequence {
        return get().firstOrNull { it.name == name }
            ?: throw RuntimeException(
                "There is no sequence with name '${name}' among " + get().map { it.name })
    }

    companion object {
        fun from(list: List<Sequence>) : SequenceProvider {
            return object : SequenceProvider {
                override fun get(): List<Sequence> {
                    return list
                }
            }
        }
    }
}
