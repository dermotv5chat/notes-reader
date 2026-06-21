package com.andriod.reader.data.local

import com.andriod.reader.data.repository.SyncPathUtils

object NotePathNames {
    fun validateSegment(raw: String): String {
        val name = raw.trim().removeSuffix(".md")
        require(name.isNotEmpty()) { "名称不能为空" }
        require(!name.contains('/')) { "名称不能包含 /" }
        require(!name.contains('\\')) { "名称不能包含 \\" }
        require(name != "." && name != "..") { "名称无效" }
        return name
    }

    fun noteBaseName(fileName: String): String =
        fileName.substringAfterLast('/').removeSuffix(".md")

    fun parentPath(fileName: String): String =
        fileName.substringBeforeLast('/', "")

    fun buildNotePath(parentFolder: String, baseName: String): String {
        val parent = SyncPathUtils.normalize(parentFolder)
        val name = validateSegment(baseName)
        return if (parent.isEmpty()) "$name.md" else "$parent/$name.md"
    }

    fun buildFolderPath(parentFolder: String, folderName: String): String {
        val parent = SyncPathUtils.normalize(parentFolder)
        val name = validateSegment(folderName)
        return if (parent.isEmpty()) name else "$parent/$name"
    }

    fun newNoteFileName(parentFolder: String): String {
        val baseName = MarkdownParser.newFileName().removeSuffix(".md")
        return buildNotePath(parentFolder, baseName)
    }
}
