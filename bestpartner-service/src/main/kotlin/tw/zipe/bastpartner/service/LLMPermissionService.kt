package tw.zipe.bastpartner.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.dto.PermissionDTO
import tw.zipe.bastpartner.entity.LLMPermissionEntity
import tw.zipe.bastpartner.enumerate.UserStatus
import tw.zipe.bastpartner.repository.LLMPermissionRepository

/**
 * @author Gary
 * @created 2024/12/18
 */
@ApplicationScoped
class LLMPermissionService(
    private val llmPermissionRepository: LLMPermissionRepository
) {

    fun add(permissionDTO: PermissionDTO) {
        with(LLMPermissionEntity()) {
            name = permissionDTO.name.orEmpty()
            num = permissionDTO.num
            description = permissionDTO.description
            llmPermissionRepository.saveOrUpdate(this).let { permissionDTO.id = id }
        }
    }

    fun update(permissionDTO: PermissionDTO) = llmPermissionRepository.updatePermission(
        permissionDTO.id.orEmpty(),
        permissionDTO.name.orEmpty(),
        permissionDTO.num!!,
        permissionDTO.description.orEmpty()
    )

    fun findUserPermissionByStatus(id: String, status: UserStatus) = llmPermissionRepository.findUserPermissionByStatus(id, status)
    
    @Transactional
    fun delete(id: String) = llmPermissionRepository.deleteById(id)
}
