package tw.zipe.basepartner.properties

import io.quarkus.runtime.annotations.ConfigDocMapKey
import io.quarkus.runtime.annotations.ConfigDocSection
import io.quarkus.runtime.annotations.ConfigPhase
import io.quarkus.runtime.annotations.ConfigRoot
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithParentName
import java.util.Optional

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "ai-platform.ollama")
interface AIPlatformOllamaConfig {
    @WithParentName
    fun defaultConfig(): Optional<OllamaProp>

    @ConfigDocSection
    @ConfigDocMapKey("chat-model-name")
    @WithParentName
    fun namedConfig(): Map<String, OllamaProp>
}
