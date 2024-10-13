package tw.zipe.basepartner.properties

import io.quarkus.runtime.annotations.ConfigGroup
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.time.Duration
import java.util.Optional

@ConfigGroup
interface BaseVector {

    @WithName("url")
    fun url(): String

    @WithName("collection-name")
    fun collectionName(): Optional<String>

    @WithName("dimension")
    fun dimension(): Optional<Int>

}
