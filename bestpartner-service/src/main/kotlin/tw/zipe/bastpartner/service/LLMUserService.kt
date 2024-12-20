package tw.zipe.bastpartner.service

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Duration
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.entity.LLMUserEntity
import tw.zipe.bastpartner.entity.LLMUserRole
import tw.zipe.bastpartner.enumerate.UserStatus
import tw.zipe.bastpartner.repository.LLMUserRepository
import tw.zipe.bastpartner.repository.LLMUserRoleRepository
import tw.zipe.bastpartner.util.CryptoUtils

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMUserService(
    val llmUserRepository: LLMUserRepository,
    val llmUserRoleRepository: LLMUserRoleRepository
) {

    @Transactional(rollbackOn = [Exception::class])
    fun register(userDTO: UserDTO) {
        val userEntity = LLMUserEntity().apply {
            username = userDTO.username
            password = CryptoUtils.sha512(userDTO.password.orEmpty())
            email = userDTO.email.orEmpty()
            nickname = userDTO.nickname.orEmpty()
            phone = userDTO.phone.orEmpty()
            avatar = userDTO.avatar.orEmpty()
            status = UserStatus.ACTIVE.ordinal.toString()
        }
        llmUserRepository.saveOrUpdate(userEntity).also {
            userDTO.id = userEntity.id
            userDTO.status = userEntity.status.toInt()
        }

        val userRole = LLMUserRole().apply {
            id.userId = userDTO.id!!
            id.roleNum = 1 // 0: ADMIN, 1: USER, 2: PRO_USER
        }
        llmUserRoleRepository.saveOrUpdate(userRole)
    }

    fun findUserById(userId: String): UserDTO {
        return llmUserRepository.findUserInfo(userId) ?: UserDTO()
    }

    fun loginVerification(email: String, password: String): String? {
        return llmUserRepository.findUserByEmail(email).takeIf { it?.password == CryptoUtils.sha512(password) }
            .let { it?.id }
    }

    fun updateUser(userDTO: UserDTO) {
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
    }

    @Transactional(rollbackOn = [Exception::class])
    fun deleteUser(userId: String): Boolean {
        return llmUserRepository.deleteById(userId).also {
            llmUserRoleRepository.deleteByUserId(userId)
        }
    }

    fun generateJwtToken(userDTO: UserDTO): String {
        val permissions = setOf("VIEW_ADMIN_DETAILS", "SEND_MESSAGE", "CREATE_USER")
        return Jwt.issuer("my-issuer")
            .upn("Gary")
            .groups(permissions)
            .expiresIn(Duration.ofDays(120))
            .claim("測試", "test")
            .sign();
    }
}
