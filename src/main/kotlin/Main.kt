import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val BASE_URL = "http://127.0.0.1:10999/api/slow"
private val gson = Gson()
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
    .build()

private suspend fun getPosts(): List<Post> = makeRequest("$BASE_URL/posts", client, object : TypeToken<List<Post>>() {})
private suspend fun getComments(postId: Long): List<Comment> =
    makeRequest("$BASE_URL/posts/$postId/comments", client, object : TypeToken<List<Comment>>() {})

private suspend fun getAuthor(authorId: Long): Author =
    makeRequest("$BASE_URL/authors/$authorId", client, object : TypeToken<Author>() {})

suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        client.apiCall(url)
            .let { response ->
                if (!response.isSuccessful) {
                    response.close()
                    throw RuntimeException(response.message)
                }
                val body = response.body ?: throw RuntimeException("response body is null")
                gson.fromJson(body.string(), typeToken.type)
            }
    }

fun main(args: Array<String>) {
    CoroutineScope(EmptyCoroutineContext).launch {
        val postWithComments: List<PostWithComments> = getPosts().map {
            PostWithComments(it, getComments(it.id))
        }
        val postWithCommentsAndAuthors: List<PostWithCommentsAndAuthors> = postWithComments.map {
            PostWithCommentsAndAuthors(PostWithAuthor(it.post, getAuthor(it.post.authorId)), it.comments)
        }

    }
    Thread.sleep(10000)
}

suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }
}