package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder

/**
 * @author Gary
 * @created 2025/3/27
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class McpServerServiceTest {

    @Inject
    lateinit var mcpServerService: McpServerService


}
