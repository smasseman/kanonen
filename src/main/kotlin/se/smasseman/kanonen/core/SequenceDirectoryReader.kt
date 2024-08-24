package se.smasseman.kanonen.core

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class SequenceDirectoryReader(private val directory: File) : LogUtil {

    init {
        require(directory.isDirectory) {
            throw IllegalArgumentException("$directory is not a directory")
        }
    }

    fun readDirectory(): List<Sequence> {
        return readDirectory(directory)
    }

    private fun readDirectory(directory: File): List<Sequence> {
        logger().info("Read sequences in $directory")
        return (directory.listFiles()
            ?: throw RuntimeException("Could not list files in directory $directory"))
            .filter { it.isFile }
            .filter { it.name.endsWith(".seq") }
            .map {
                SequenceReader(SequenceName(it.name.substringBeforeLast("."))).read(
                    Files.readString(it.toPath())
                )
            }
            .toList()
    }

    fun update(sequenceName: SequenceName, code: String) {
        val file = file(sequenceName)
        logger().info("Update content in $file")
        logger().info("Code:\n$code")
        if (!file.exists()) {
            throw FileNotFoundException(file.toString())
        }
        SequenceReader(sequenceName).read(code)
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
