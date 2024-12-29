package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import tw.zipe.bastpartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/12/23
 */
@Entity
@Table(name = "llm_platform")
data class LLMPlatformEntity(
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    /**
     * LLM 平台名稱
     */
    @Column(name = "name", nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    var name: Platform? = null
) : BaseEntity()
