package tw.zipe.bastpartner.properties

import io.quarkus.runtime.annotations.ConfigGroup
import io.smallrye.config.WithName
import java.time.Duration
import java.util.Optional

@ConfigGroup
interface BaseAIPlatform {

    @WithName("url")
    fun url(): Optional<String>

    @WithName("api-key")
    fun apiKey(): Optional<String>

    @WithName("model-name")
    fun modelName(): String

    @WithName("temperature")
    fun temperature(): Double

    @WithName("timeout")
    fun timeout(): Duration

    @WithName("top-p")
    fun topP(): Optional<Double>

    @WithName("top-k")
    fun topK(): Optional<Int>

    @WithName("embedding-model-name")
    fun embeddingModelName(): Optional<String>

    @WithName("log-requests")
    fun logRequests(): Optional<Boolean>

    @WithName("log-responses")
    fun logResponses(): Optional<Boolean>

}
