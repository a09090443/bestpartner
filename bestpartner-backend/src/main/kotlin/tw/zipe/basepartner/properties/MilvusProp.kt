package tw.zipe.basepartner.properties

import io.quarkus.runtime.annotations.ConfigGroup
import io.smallrye.config.WithName
import java.util.Optional

@ConfigGroup
interface MilvusProp : BaseVector{

    @WithName("dimension")
    fun dimension(): Optional<Int>

}
