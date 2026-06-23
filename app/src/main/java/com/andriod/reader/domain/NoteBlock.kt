package com.andriod.reader.domain

sealed class NoteBlock {
    abstract val id: String
    abstract val lineIndex: Int
    abstract val rawLine: String
    abstract val trackable: Boolean

    data class Heading(
        override val id: String,
        override val lineIndex: Int,
        override val rawLine: String,
        val level: Int,
        val text: String,
    ) : NoteBlock() {
        override val trackable: Boolean = false
    }

    data class Paragraph(
        override val id: String,
        override val lineIndex: Int,
        override val rawLine: String,
        val text: String,
    ) : NoteBlock() {
        override val trackable: Boolean = false
    }

    data class Todo(
        override val id: String,
        override val lineIndex: Int,
        override val rawLine: String,
        val checked: Boolean,
        val text: String,
    ) : NoteBlock() {
        override val trackable: Boolean = false
    }

    data class Callout(
        override val id: String,
        override val lineIndex: Int,
        override val rawLine: String,
        val variant: String,
        val text: String,
    ) : NoteBlock() {
        override val trackable: Boolean = true
    }

    data class Bullet(
        override val id: String,
        override val lineIndex: Int,
        override val rawLine: String,
        val text: String,
    ) : NoteBlock() {
        override val trackable: Boolean = false
    }

    data class Ordered(
        override val id: String,
        override val lineIndex: Int,
        override val rawLine: String,
        val number: String,
        val text: String,
    ) : NoteBlock() {
        override val trackable: Boolean = false
    }
}

fun NoteBlock.shouldDisplayInReader(): Boolean = when (this) {
    is NoteBlock.Heading -> text.isNotBlank()
    is NoteBlock.Paragraph -> text.isNotBlank()
    is NoteBlock.Todo -> text.isNotBlank()
    is NoteBlock.Callout -> text.isNotBlank()
    is NoteBlock.Bullet -> text.isNotBlank()
    is NoteBlock.Ordered -> text.isNotBlank()
}
