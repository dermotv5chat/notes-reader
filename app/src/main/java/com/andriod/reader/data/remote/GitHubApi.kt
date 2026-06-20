package com.andriod.reader.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class GitHubRepoResponse(
    @SerializedName("full_name") val fullName: String,
    val private: Boolean,
    @SerializedName("default_branch") val defaultBranch: String,
)

interface GitHubApi {
    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authorization: String,
    ): GitHubRepoResponse

    @GET("repos/{owner}/{repo}/contents")
    suspend fun listRootContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authorization: String,
    ): List<GitHubContentItem>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authorization: String,
    ): GitHubContentResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun listContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authorization: String,
    ): List<GitHubContentItem>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authorization: String,
        @Body body: GitHubPutRequest,
    ): GitHubPutResponse

    @DELETE("repos/{owner}/{repo}/contents/{path}")
    suspend fun deleteContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authorization: String,
        @Query("message") message: String,
        @Query("sha") sha: String,
    )
}

data class GitHubContentResponse(
    val name: String,
    val path: String,
    val sha: String,
    val content: String,
    val encoding: String,
)

data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val type: String,
    val downloadUrl: String? = null,
)

data class GitHubPutRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
)

data class GitHubPutResponse(
    val content: GitHubContentItem?,
    val commit: GitHubCommit?,
)

data class GitHubCommit(
    val sha: String,
)

data class GitHubErrorResponse(
    @SerializedName("message") val message: String?,
)
