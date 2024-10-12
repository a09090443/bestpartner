package tw.zipe.basepartner.repository

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test

/**
 * @author Gary
 * @created 2024/10/12
 */
@QuarkusTest
class AigcModelRepositoryTest {

    @Inject
    lateinit var aigcModelRepository: AigcModelRepository

    @Test
    fun findAllTest() {
        val movies = aigcModelRepository.findAll().list()
        println(movies)
    }
}
