package com.andriod.reader.data.repository

import android.util.Base64
import com.andriod.reader.data.local.MarkdownParser
import com.andriod.reader.data.local.NoteFileStore
import com.andriod.reader.data.local.SyncStateStore
import com.andriod.reader.data.remote.GitHubApi
import com.andriod.reader.data.remote.GitHubContentItem
import com.andriod.reader.data.remote.GitHubPutRequest
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.ConflictAction
import com.andriod.reader.domain.GitHubSettings
import com.andriod.reader.domain.SyncConflict
import com.andriod.reader.domain.SyncFileState
import com.andriod.reader.domain.SyncResult
import com.andriod.reader.domain.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val settingsStore: SettingsStore,
    private val noteFileStore: NoteFileStore,
    private val syncStateStore: SyncStateStore,
) {
    suspend fun testConnectionMessage(): String = when (val result = testConnectionInternal()) {
        is ConnectionTestResult.Success -> result.message
        is ConnectionTestResult.Error -> result.message
    }

    private suspend fun testConnectionInternal(): ConnectionTestResult = withContext(Dispatchers.IO) {
        val token = settingsStore.getToken()
            ?: return@withContext ConnectionTestResult.Error("请先填写 GitHub PAT")
        val settings = settingsStore.getGitHubSettings()
        val auth = authHeader(token)

        try {
            val repo = gitHubApi.getRepo(settings.owner, settings.repo, auth)
            val (path, items) = resolveRemoteMarkdownItems(settings, auth)
            ConnectionTestResult.Success(
                "连接成功：${repo.fullName}\n" +
                    "笔记目录：${path.ifBlank { "仓库根目录" }}，发现 ${items.size} 个 .md 文件",
            )
        } catch (e: HttpException) {
            ConnectionTestResult.Error(parseHttpError(e, settings))
        } catch (e: Exception) {
            ConnectionTestResult.Error(e.message ?: "连接失败")
        }
    }

    suspend fun uploadPending(): SyncResult = withContext(Dispatchers.IO) {
        val token = settingsStore.getToken()
            ?: return@withContext SyncResult.Error("请先在设置中配置 GitHub PAT")
        val settings = settingsStore.getGitHubSettings()
        val auth = authHeader(token)
        val states = syncStateStore.readAll().toMutableMap()
        var uploaded = 0
        var deleted = 0

        try {
            gitHubApi.getRepo(settings.owner, settings.repo, auth)
            val effectivePath = resolveNotesPath(settings, auth)

            states.filter { (_, state) ->
                state.pendingDelete || state.syncStatus == SyncStatus.PENDING || state.syncStatus == SyncStatus.LOCAL_ONLY
            }.forEach { (fileName, state) ->
                val remotePath = remoteFilePath(effectivePath, fileName)
                if (state.pendingDelete) {
                    val sha = state.githubSha
                    if (sha != null) {
                        gitHubApi.deleteContent(
                            owner = settings.owner,
                            repo = settings.repo,
                            path = remotePath,
                            authorization = auth,
                            message = "Delete note $fileName",
                            sha = sha,
                        )
                        noteFileStore.deleteLocalFile(fileName)
                        states.remove(fileName)
                        deleted++
                    }
                } else {
                    val raw = noteFileStore.readRawFile(fileName) ?: return@forEach
                    val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val response = gitHubApi.putContent(
                        owner = settings.owner,
                        repo = settings.repo,
                        path = remotePath,
                        authorization = auth,
                        body = GitHubPutRequest(
                            message = "Update note $fileName",
                            content = encoded,
                            sha = state.githubSha,
                        ),
                    )
                    states[fileName] = SyncFileState(
                        githubSha = response.content?.sha,
                        syncStatus = SyncStatus.SYNCED,
                        pendingDelete = false,
                    )
                    uploaded++
                }
            }
            syncStateStore.writeAll(states)
            SyncResult.Success(uploaded = uploaded, downloaded = 0, deleted = deleted)
        } catch (e: HttpException) {
            SyncResult.Error(parseHttpError(e, settings))
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "上传失败")
        }
    }

    suspend fun downloadRemote(
        conflictResolver: suspend (SyncConflict) -> ConflictAction = { ConflictAction.KeepRemote },
    ): SyncResult = withContext(Dispatchers.IO) {
        val token = settingsStore.getToken()
            ?: return@withContext SyncResult.Error("请先在设置中配置 GitHub PAT")
        val settings = settingsStore.getGitHubSettings()
        val auth = authHeader(token)
        var downloaded = 0

        try {
            gitHubApi.getRepo(settings.owner, settings.repo, auth)
            val (_, remoteItems) = resolveRemoteMarkdownItems(settings, auth)

            if (remoteItems.isEmpty()) {
                return@withContext SyncResult.Error(
                    "远程没有找到 .md 笔记文件。请检查「笔记目录路径」是否正确（可尝试留空表示仓库根目录）。",
                )
            }

            val states = syncStateStore.readAll().toMutableMap()

            remoteItems.forEach { item ->
                val response = gitHubApi.getContents(
                    owner = settings.owner,
                    repo = settings.repo,
                    path = item.path,
                    authorization = auth,
                )
                val remoteRaw = decodeContent(response.content)
                val remoteParsed = MarkdownParser.parse(item.name, remoteRaw)
                val localRaw = noteFileStore.readRawFile(item.name)
                val localState = states[item.name]

                if (localRaw != null && localState?.syncStatus == SyncStatus.PENDING) {
                    val localParsed = MarkdownParser.parse(item.name, localRaw)
                    if (remoteParsed.updatedAt.isAfter(localParsed.updatedAt)) {
                        when (val action = conflictResolver(
                            SyncConflict(item.name, localParsed.updatedAt, remoteParsed.updatedAt),
                        )) {
                            ConflictAction.KeepLocal -> return@forEach
                            ConflictAction.KeepRemote -> {
                                noteFileStore.writeRawFile(item.name, remoteRaw)
                                states[item.name] = SyncFileState(
                                    githubSha = response.sha,
                                    syncStatus = SyncStatus.SYNCED,
                                )
                                downloaded++
                            }
                            is ConflictAction.SaveCopy -> {
                                noteFileStore.writeRawFile(action.newFileName, localRaw)
                                noteFileStore.writeRawFile(item.name, remoteRaw)
                                states[item.name] = SyncFileState(
                                    githubSha = response.sha,
                                    syncStatus = SyncStatus.SYNCED,
                                )
                                downloaded++
                            }
                        }
                    }
                } else {
                    noteFileStore.writeRawFile(item.name, remoteRaw)
                    states[item.name] = SyncFileState(
                        githubSha = response.sha,
                        syncStatus = SyncStatus.SYNCED,
                    )
                    downloaded++
                }
            }

            syncStateStore.writeAll(states)
            SyncResult.Success(uploaded = 0, downloaded = downloaded, deleted = 0)
        } catch (e: HttpException) {
            SyncResult.Error(parseHttpError(e, settings))
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "下载失败")
        }
    }

    private suspend fun resolveRemoteMarkdownItems(
        settings: GitHubSettings,
        auth: String,
    ): Pair<String, List<GitHubContentItem>> {
        val configured = settings.notesPath.trim()
        if (configured.isNotEmpty()) {
            val items = listMarkdownAt(settings, configured, auth)
            if (items.isNotEmpty()) return configured to items
        }
        val rootItems = listMarkdownAt(settings, "", auth)
        if (rootItems.isNotEmpty()) return "" to rootItems
        if (configured.isNotEmpty()) {
            return configured to emptyList()
        }
        return "" to emptyList()
    }

    private suspend fun resolveNotesPath(settings: GitHubSettings, auth: String): String {
        val configured = settings.notesPath.trim()
        if (configured.isNotEmpty() && listMarkdownAt(settings, configured, auth).isNotEmpty()) {
            return configured
        }
        if (listMarkdownAt(settings, "", auth).isNotEmpty()) {
            return ""
        }
        return configured
    }

    private suspend fun listMarkdownAt(
        settings: GitHubSettings,
        path: String,
        auth: String,
    ): List<GitHubContentItem> {
        return try {
            val items = if (path.isBlank()) {
                gitHubApi.listRootContents(settings.owner, settings.repo, auth)
            } else {
                gitHubApi.listContents(settings.owner, settings.repo, path, auth)
            }
            items.filter { it.type == "file" && it.name.endsWith(".md", ignoreCase = true) }
        } catch (e: HttpException) {
            if (e.code() == 404) emptyList() else throw e
        }
    }

    private fun remoteFilePath(notesPath: String, fileName: String): String {
        val path = notesPath.trim()
        return if (path.isEmpty()) fileName else "$path/$fileName"
    }

    private fun authHeader(token: String): String = "Bearer ${token.trim()}"

    private fun decodeContent(content: String): String {
        val cleaned = content.replace("\n", "")
        val bytes = Base64.decode(cleaned, Base64.DEFAULT)
        return String(bytes, Charsets.UTF_8)
    }

    private fun parseHttpError(e: HttpException, settings: GitHubSettings): String {
        return when (e.code()) {
            401 -> "GitHub Token 无效或已过期，请重新生成 PAT"
            404 -> "无法访问 ${settings.owner}/${settings.repo}。\n" +
                "请确认：\n" +
                "1. 仓库名和 Owner 正确\n" +
                "2. Fine-grained Token 已授权该仓库，并勾选 Contents 读写权限\n" +
                "3. Classic Token 已勾选 repo 权限"
            403 -> "Token 权限不足，请为仓库开启 Contents 读写权限"
            else -> e.response()?.errorBody()?.string() ?: "GitHub 请求失败 (${e.code()})"
        }
    }

    private sealed class ConnectionTestResult {
        data class Success(val message: String) : ConnectionTestResult()
        data class Error(val message: String) : ConnectionTestResult()
    }
}
