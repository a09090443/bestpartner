package tw.zipe.bastpartner.util

import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Utility class for HTTP requests using OkHttp in Kotlin.
 */
object OkHttpUtil {

    // Timeout settings in seconds
    private const val READ_TIMEOUT = 100L
    private const val CONNECT_TIMEOUT = 60L
    private const val WRITE_TIMEOUT = 60L

    // Media types for JSON and XML
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val XML = "application/xml; charset=utf-8".toMediaType()

    private var okHttpClient: OkHttpClient? = null

    /**
     * Provides a singleton OkHttpClient instance with custom configurations.
     * Configured to trust all certificates (use with caution in production).
     */
    private fun getOkHttpClient(): OkHttpClient {
        if (okHttpClient == null) {
            synchronized(this) {
                if (okHttpClient == null) {
                    val trustManagers = arrayOf<TrustManager>(TrustAllCerts())
                    val sslContext = SSLContext.getInstance("TLS").apply {
                        init(null, trustManagers, SecureRandom())
                    }

                    okHttpClient = OkHttpClient.Builder()
                        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                        .sslSocketFactory(sslContext.socketFactory, trustManagers[0] as X509TrustManager)
                        .hostnameVerifier { _, _ -> true }
                        .build()
                }
            }
        }
        return okHttpClient!!
    }

    /**
     * Synchronous GET request.
     * @param url The URL to send the request to.
     * @return The Response object, or null if the request fails.
     */
    fun getData(url: String): Response? {
        val request = Request.Builder().url(url).build()
        return getOkHttpClient().newCall(request).execute()
    }

    /**
     * Synchronous POST request with form-encoded parameters.
     * @param url The URL to send the request to.
     * @param bodyParams A map of key-value pairs to include in the request body.
     * @return The Response object, or null if the request fails.
     */
    fun postData(url: String, bodyParams: Map<String, String>): Response? {
        val body = setRequestBody(bodyParams)
        val request = Request.Builder().post(body).url(url).build()
        return getOkHttpClient().newCall(request).execute()
    }

    /**
     * Asynchronous GET request.
     * @param url The URL to send the request to.
     * @param netCall Callback interface for handling success and failure.
     */
    fun getDataAsyn(url: String, netCall: NetCall) {
        val request = Request.Builder().url(url).build()
        getOkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                netCall.failed(call, e)
            }

            override fun onResponse(call: Call, response: Response) {
                netCall.success(call, response)
            }
        })
    }

    /**
     * Asynchronous POST request with form-encoded parameters.
     * @param url The URL to send the request to.
     * @param bodyParams A map of key-value pairs to include in the request body.
     * @param netCall Callback interface for handling success and failure.
     */
    fun postDataAsyn(url: String, bodyParams: Map<String, String>, netCall: NetCall) {
        val body = setRequestBody(bodyParams)
        val request = Request.Builder().post(body).url(url).build()
        getOkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                netCall.failed(call, e)
            }

            override fun onResponse(call: Call, response: Response) {
                netCall.success(call, response)
            }
        })
    }

    /**
     * Helper function to build a request body from a map of parameters.
     * @param bodyParams A map of key-value pairs to include in the request body.
     * @return A RequestBody object for the POST request.
     */
    private fun setRequestBody(bodyParams: Map<String, String>): RequestBody {
        val formEncodingBuilder = FormBody.Builder()
        for ((key, value) in bodyParams) {
            formEncodingBuilder.add(key, value)
        }
        return formEncodingBuilder.build()
    }

    /**
     * Synchronous POST request with XML data.
     * @param url The URL to send the request to.
     * @param xml The XML string to include in the request body.
     * @return The response body as a string.
     * @throws IOException if the request fails.
     */
    fun postXml(url: String, xml: String): String {
        val body = xml.toRequestBody(XML)
        val request = Request.Builder().url(url).post(body).build()
        getOkHttpClient().newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body!!.string()
            } else {
                throw IOException("Unexpected code ${response.code}")
            }
        }
    }

    /**
     * Synchronous POST request with JSON data.
     * @param url The URL to send the request to.
     * @param json The JSON string to include in the request body.
     * @return The response body as a string.
     * @throws IOException if the request fails.
     */
    fun postJson(url: String, json: String): String {
        val body = json.toRequestBody(JSON)
        val request = Request.Builder().url(url).post(body).build()
        getOkHttpClient().newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body!!.string()
            } else {
                throw IOException("Unexpected code ${response.code}")
            }
        }
    }

    /**
     * Asynchronous POST request with JSON data.
     * @param url The URL to send the request to.
     * @param json The JSON string to include in the request body.
     * @param netCall Callback interface for handling success and failure.
     */
    fun postJsonAsyn(url: String, json: String, netCall: NetCall) {
        val body = json.toRequestBody(JSON)
        val request = Request.Builder().post(body).url(url).build()
        getOkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                netCall.failed(call, e)
            }

            override fun onResponse(call: Call, response: Response) {
                netCall.success(call, response)
            }
        })
    }

    /**
     * A TrustManager that trusts all certificates (for development purposes only).
     */
    private class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    /**
     * Callback interface for handling asynchronous HTTP request results.
     */
    interface NetCall {
        /** Called when the request succeeds. */
        fun success(call: Call, response: Response)

        /** Called when the request fails. */
        fun failed(call: Call, e: IOException)
    }
}

fun main() {
    try {
        // 使用 OkHttpUtil 進行 GET 請求
        val getResponse = OkHttpUtil.getData("https://jsonplaceholder.typicode.com/posts/1")
        if (getResponse != null) {
            println("GET Response: ${getResponse.body?.string()}")
        }

        // 使用 OkHttpUtil 進行 POST 請求
        val postParams = mapOf("title" to "foo", "body" to "bar", "userId" to "1")
        val postResponse = OkHttpUtil.postData("https://jsonplaceholder.typicode.com/posts", postParams)
        if (postResponse != null) {
            println("POST Response: ${postResponse.body?.string()}")
        }
    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
    }
}
