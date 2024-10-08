package tw.zipe.basepartner.properties

import io.quarkus.runtime.annotations.ConfigGroup
import io.smallrye.config.WithName
import java.time.Duration
import java.util.Optional

@ConfigGroup
interface BaseAIPlatform {

    @WithName("url")
    fun url(): String

    @WithName("model-name")
    fun modelName(): String

    @WithName("temperature")
    fun temperature(): Double

    @WithName("timeout")
    fun timeout(): Duration

    @WithName("embedding-model")
    fun embeddingModel(): Optional<String>
}
