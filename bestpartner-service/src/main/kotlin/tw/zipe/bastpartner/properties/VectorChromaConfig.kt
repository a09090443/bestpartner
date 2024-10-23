package tw.zipe.bastpartner.properties

import io.quarkus.runtime.annotations.ConfigDocMapKey
import io.quarkus.runtime.annotations.ConfigDocSection
import io.quarkus.runtime.annotations.ConfigPhase
import io.quarkus.runtime.annotations.ConfigRoot
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithParentName
import java.util.Optional

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "vector.chroma")
interface VectorChromaConfig {
    @WithParentName
    fun defaultConfig(): Optional<ChromaProp>

    @ConfigDocSection
    @ConfigDocMapKey("vector-name")
    @WithParentName
    fun namedConfig(): Map<String, ChromaProp>
}
