package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2025/04/18
 */
@Entity
@Table(name = "llm_skill_resource")
class LLMSkillResourceEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "skill_id", nullable = false)
    var skillId: String = ""

    @Column(name = "relative_path", nullable = false)
    var relativePath: String = ""

    @Column(name = "content", nullable = true, columnDefinition = "text")
    var content: String? = null
}
