package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2025/04/18
 */
@Serializable
class SkillDTO(
    var id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val content: String? = null,
    val dirPath: String? = null,
    val isGlobal: Boolean = false,
    val resources: List<SkillResourceDTO>? = null
)

@Serializable
class SkillResourceDTO(
    var id: String? = null,
    val skillId: String? = null,
    val relativePath: String? = null,
    val content: String? = null
)
