package tw.zipe.bastpartner.service
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.enumerate.UserStatus

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LLMUserServiceTest {

    @Inject
    lateinit var llmUserService: LLMUserService

    @Test
    @Order(1)
    fun `test register user`() {
        llmUserService.register(testUserDTO)

        assertNotNull(testUserDTO.id)
    }

    @Test
    @Order(2)
    fun `test get user`() {
        val retrievedUser = llmUserService.getUser(testUserDTO.id!!)

        assertEquals(testUserDTO.id, retrievedUser.id)
    }

    @Test
    @Order(3)
    fun `test updateUser`() {
        testUserDTO.email = "example@example.com"
        testUserDTO.phone = "0987654321"
        testUserDTO.status = UserStatus.ACTIVE
        llmUserService.updateUser(testUserDTO)
        val userDTO = llmUserService.getUser(testUserDTO.id!!)
        assertEquals(testUserDTO.email, userDTO.email)
        assertEquals(testUserDTO.phone, userDTO.phone)
        assertEquals(testUserDTO.status, userDTO.status)
    }

    @Test
    @Order(4)
    fun `test delete user`() {
        val result = llmUserService.deleteUser(testUserDTO.id!!)
        assertTrue(result)
        val user = llmUserService.getUser(testUserDTO.id!!)
        assertNull(user.id)
    }

    //    @Test
    fun `test generate JWT token`() {
        val token = llmUserService.generateJwtToken(testUserDTO)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    //    @Test
    fun `test get non-existent user throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            llmUserService.getUser("non-existent-id")
        }
    }


    companion object {
        lateinit var testUserDTO: UserDTO

        @BeforeAll
        @JvmStatic
        fun init() {
            testUserDTO = UserDTO(
                username = "testuser",
                password = "password123",
                email = "test@example.com",
                nickname = "Test User",
                phone = "1234567890",
                avatar = "avatar.jpg"
            )
        }
    }
}
