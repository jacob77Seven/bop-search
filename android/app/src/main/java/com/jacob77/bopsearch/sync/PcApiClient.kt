package com.jacob77.bopsearch.sync

import android.util.Log
import com.jacob77.bopsearch.data.PeerSettings
import com.jacob77.bopsearch.data.QueueItemEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "BopPc"

data class HealthResult(val online: Boolean, val body: String? = null, val error: String? = null)

data class JobPostResult(val ok: Boolean, val jobId: String? = null, val error: String? = null)

class PcApiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build(),
) {
    fun baseUrl(settings: PeerSettings): String {
        val host = settings.host.trim().trimEnd('/')
        return "http://$host:${settings.port}"
    }

    suspend fun probeHealth(settings: PeerSettings): HealthResult = withContext(Dispatchers.IO) {
        val urls = listOf(
            "${baseUrl(settings)}/health",
            "${baseUrl(settings)}/v1/status",
        )
        var lastError: String? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).get().build()
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        Log.i(TAG, "health ok url=$url body=$body")
                        return@withContext HealthResult(online = true, body = body)
                    }
                    lastError = "HTTP ${response.code} at $url"
                    Log.w(TAG, lastError!!)
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
                Log.d(TAG, "health miss url=$url err=$lastError")
            }
        }
        return@withContext HealthResult(online = false, error = lastError)
    }

    suspend fun postJob(settings: PeerSettings, item: QueueItemEntity): JobPostResult = withContext(Dispatchers.IO) {
        val url = "${baseUrl(settings)}/v1/jobs"
        val metadata = JSONObject().apply {
            put("curationNotes", item.curationNotes)
            if (item.rating != null) put("rating", item.rating)
            if (item.curationAction != null) put("curationAction", item.curationAction)
            put("createdAtEpochMs", item.createdAtEpochMs)
        }
        val payload = JSONObject().apply {
            put("prompt", item.prompt)
            put("metadata", metadata)
            put("client_id", settings.clientId)
            put("local_id", item.id.toString())
            put("kind", item.kind)
        }
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        return@withContext try {
            val request = Request.Builder().url(url).post(body).build()
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = "HTTP ${response.code}: $text"
                    Log.w(TAG, "postJob failed id=${item.id} $err")
                    return@withContext JobPostResult(ok = false, error = err)
                }
                val jobId = runCatching { JSONObject(text).optString("id").ifEmpty { null } }
                    .getOrNull()
                Log.i(TAG, "postJob ok local=${item.id} remote=$jobId")
                JobPostResult(ok = true, jobId = jobId ?: "unknown")
            }
        } catch (e: Exception) {
            val err = e.message ?: e.javaClass.simpleName
            Log.w(TAG, "postJob error id=${item.id} $err")
            JobPostResult(ok = false, error = err)
        }
    }
}
