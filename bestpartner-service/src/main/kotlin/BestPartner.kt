package tw.zipe.bastpartner

import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import java.util.logging.Logger

/**
 * @author Gary
 * @created 2025/4/12
 */
@QuarkusMain
class BestPartner : QuarkusApplication {
    companion object {
        private val logger = Logger.getLogger(BestPartner::class.java.name)
    }

    override fun run(vararg args: String?): Int {
        logger.info("啟動 BestPartner AI應用平台...")
        Quarkus.waitForExit()
        return 0
    }
}

fun main(args: Array<String>) {
    System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    Quarkus.run(BestPartner::class.java, *args)
}
