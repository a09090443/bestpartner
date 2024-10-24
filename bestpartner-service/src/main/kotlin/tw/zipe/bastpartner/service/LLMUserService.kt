package tw.zipe.bastpartner.service

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.repository.LLMUserRepository

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMUserService(
    val llmUserRepository: LLMUserRepository
) {

    fun register(userDTO: UserDTO): String {
        return "success"
    }

    fun getUser(username:String): String {
        return llmUserRepository.findByUsername("Gary")?.username ?: ""
    }
}
