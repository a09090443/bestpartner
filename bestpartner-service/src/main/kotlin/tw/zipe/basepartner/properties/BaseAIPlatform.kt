package tw.zipe.basepartner.properties

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

    @WithName("embedding-model-name")
    fun embeddingModelName(): Optional<String>
}
