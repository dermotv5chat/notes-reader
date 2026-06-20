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
            val items = resolveRemoteMarkdownItems(settings, auth)
            ConnectionTestResult.Success(
                "连接成功：${repo.fullName}\n" +
                    "扫描整个仓库，发现 ${items.size} 个 .md 文件（含子目录）",
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

            states.filter { (_, state) ->
                state.pendingDelete || state.syncStatus == SyncStatus.PENDING || state.syncStatus == SyncStatus.LOCAL_ONLY
            }.forEach { (localPath, state) ->
                val remotePath = state.remotePath ?: SyncPathUtils.normalize(localPath)
                if (state.pendingDelete) {
                    val sha = state.githubSha
                    if (sha != null) {
                        gitHubApi.deleteContent(
                            owner = settings.owner,
                            repo = settings.repo,
                            path = remotePath,
                            authorization = auth,
                            message = "Delete note $localPath",
                            sha = sha,
                        )
                        if (noteFileStore.readRawFile(localPath) != null) {
                            noteFileStore.deleteLocalFile(localPath)
                        }
                        states.remove(localPath)
                        deleted++
                    }
                } else {
                    val raw = noteFileStore.readRawFile(localPath) ?: return@forEach
                    val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val response = gitHubApi.putContent(
                        owner = settings.owner,
                        repo = settings.repo,
                        path = remotePath,
                        authorization = auth,
                        body = GitHubPutRequest(
                            message = "Update note $localPath",
                            content = encoded,
                            sha = state.githubSha,
                        ),
                    )
                    states[localPath] = SyncFileState(
                        githubSha = response.content?.sha,
                        remotePath = remotePath,
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
            val remoteItems = resolveRemoteMarkdownItems(settings, auth)

            if (remoteItems.isEmpty()) {
                return@withContext SyncResult.Error("仓库中没有找到 .md 笔记文件。")
            }

            val states = syncStateStore.readAll().toMutableMap()

            remoteItems.forEach { item ->
                val localPath = SyncPathUtils.normalize(item.path)
                val response = gitHubApi.getContents(
                    owner = settings.owner,
                    repo = settings.repo,
                    path = item.path,
                    authorization = auth,
                )
                val remoteRaw = decodeContent(response.content)
                val remoteParsed = MarkdownParser.parse(localPath, remoteRaw)
                val localRaw = noteFileStore.readRawFile(localPath)
                val localState = states[localPath]

                if (localRaw != null && localState?.syncStatus == SyncStatus.PENDING) {
                    val localParsed = MarkdownParser.parse(localPath, localRaw)
                    if (remoteParsed.updatedAt.isAfter(localParsed.updatedAt)) {
                        when (val action = conflictResolver(
                            SyncConflict(localPath, localParsed.updatedAt, remoteParsed.updatedAt),
                        )) {
                            ConflictAction.KeepLocal -> return@forEach
                            ConflictAction.KeepRemote -> {
                                noteFileStore.writeRawFile(localPath, remoteRaw)
                                states[localPath] = SyncFileState(
                                    githubSha = response.sha,
                                    remotePath = item.path,
                                    syncStatus = SyncStatus.SYNCED,
                                )
                                downloaded++
                            }
                            is ConflictAction.SaveCopy -> {
                                noteFileStore.writeRawFile(action.newFileName, localRaw)
                                noteFileStore.writeRawFile(localPath, remoteRaw)
                                states[localPath] = SyncFileState(
                                    githubSha = response.sha,
                                    remotePath = item.path,
                                    syncStatus = SyncStatus.SYNCED,
                                )
                                downloaded++
                            }
                        }
                    }
                } else {
                    noteFileStore.writeRawFile(localPath, remoteRaw)
                    states[localPath] = SyncFileState(
                        githubSha = response.sha,
                        remotePath = item.path,
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
    ): List<GitHubContentItem> = listMarkdownRecursive(settings, "", auth)

    private suspend fun listMarkdownRecursive(
        settings: GitHubSettings,
        path: String,
        auth: String,
    ): List<GitHubContentItem> {
        val results = mutableListOf<GitHubContentItem>()
        collectMarkdown(settings, path, auth, results)
        return results.sortedBy { it.path }
    }

    private suspend fun collectMarkdown(
        settings: GitHubSettings,
        path: String,
        auth: String,
        results: MutableList<GitHubContentItem>,
    ) {
        val items = listContentsAt(settings, path, auth)
        for (item in items) {
            when {
                item.type == "file" && item.name.endsWith(".md", ignoreCase = true) -> results.add(item)
                item.type == "dir" -> collectMarkdown(settings, item.path, auth, results)
            }
        }
    }

    private suspend fun listContentsAt(
        settings: GitHubSettings,
        path: String,
        auth: String,
    ): List<GitHubContentItem> {
        return try {
            if (path.isBlank()) {
                gitHubApi.listRootContents(settings.owner, settings.repo, auth)
            } else {
                gitHubApi.listContents(settings.owner, settings.repo, path, auth)
            }
        } catch (e: HttpException) {
            if (e.code() == 404) emptyList() else throw e
        }
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
