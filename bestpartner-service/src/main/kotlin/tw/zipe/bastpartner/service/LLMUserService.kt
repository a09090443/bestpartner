package tw.zipe.bastpartner.service

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity_.id
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import java.util.stream.Collectors
import org.apache.poi.ss.formula.functions.T
import org.eclipse.microprofile.jwt.Claims
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.entity.LLMUserEntity
import tw.zipe.bastpartner.enumerate.UserStatus
import tw.zipe.bastpartner.repository.LLMUserRepository
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMUserService(
    val llmUserRepository: LLMUserRepository,
    val entityManager: EntityManager,
    val identity: SecurityIdentity
) {

    @Transactional
    fun register(userDTO: UserDTO): UserDTO {
        with(LLMUserEntity()) {
            username = userDTO.username
            password = CryptoUtils.sha512(userDTO.password)
            email = userDTO.email.orEmpty()
            nickname = userDTO.nickname.orEmpty()
            phone = userDTO.phone.orEmpty()
            avatar = userDTO.avatar.orEmpty()
            status = UserStatus.INACTIVE
            llmUserRepository.persist(this)
            userDTO.id = this.id
        }
        return userDTO
    }

    fun getUser(userId: String): UserDTO {
        return llmUserRepository.findById(userId)?.run {
            UserDTO(
                id = this.id,
                username = this.username,
                email = this.email,
                nickname = this.nickname,
                phone = this.phone,
                avatar = this.avatar,
                status = this.status
            )
        } ?: UserDTO()
    }

    @Transactional
    fun updateUser(userDTO: UserDTO): UserDTO {
        val paramMap = mapOf(
            "id" to userDTO.id!!,
            "username" to userDTO.username,
            "email" to userDTO.email.orEmpty(),
            "nickname" to userDTO.nickname.orEmpty(),
            "phone" to userDTO.phone.orEmpty(),
            "avatar" to userDTO.avatar.orEmpty(),
            "status" to userDTO.status!!
        )
        llmUserRepository.updateUserByNativeSQL(userDTO.id!!, paramMap)
        return UserDTO()
    }

    @Transactional
    fun deleteUser(userId: String) = llmUserRepository.deleteById(userId)

    fun generateJwtToken(userDTO: UserDTO): String {
        val permissions = setOf("VIEW_ADMIN_DETAILS", "SEND_MESSAGE", "CREATE_USER")
        return Jwt.issuer("my-issuer")
            .upn("Gary")
            .groups(permissions)
            .expiresIn(500)
            .claim("測試", "test")
            .sign();
    }
}
