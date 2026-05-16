package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import tw.zipe.bastpartner.enumerate.SkillScope

/**
 * @author Gary
 * @created 2025/04/18
 */
@Entity
@Table(name = "llm_skill")
class LLMSkillEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "user_id", nullable = false)
    var userId: String = ""

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Column(name = "description", nullable = true, columnDefinition = "text")
    var description: String? = null

    @Column(name = "content", nullable = true, columnDefinition = "text")
    var content: String? = null

    @Column(name = "dir_path", nullable = true, length = 500)
    var dirPath: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    var scope: SkillScope = SkillScope.USER
}
