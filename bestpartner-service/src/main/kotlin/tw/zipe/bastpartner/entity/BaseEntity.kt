package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import io.quarkus.arc.Arc
import io.quarkus.security.identity.SecurityIdentity
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.time.LocalDateTime

/**
 * @author Gary
 * @created 2024/10/16
 */
@MappedSuperclass
open class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime? = null

    @Column(name = "created_by", nullable = true, updatable = false)
    var createdBy: String = StringUtil.EMPTY_STRING

    @Column(name = "updated_by", nullable = true)
    var updatedBy: String = StringUtil.EMPTY_STRING

    /**
     * JPA 生命週期回呼：在 INSERT 前執行（@GeneratedValue UUID 已賦值後）
     * 統一設定 createdAt 與 createdBy，無需依賴 BaseRepository 的時序
     */
    @PrePersist
    fun onPrePersist() {
        createdAt = LocalDateTime.now()

        // 從 CDI 取得當前登入者；公開端點（無認證）回傳空字串
        val username = runCatching {
            Arc.container()
                ?.instance(SecurityIdentity::class.java)?.get()
                ?.takeUnless { it.isAnonymous }
                ?.principal?.name ?: StringUtil.EMPTY_STRING
        }.getOrDefault(StringUtil.EMPTY_STRING)

        createdBy = username.ifEmpty {
            // 公開端點 fallback：以 entity 自身 id 作為 createdBy（例如 register）
            // @PrePersist 在 GenerationType.UUID 賦值後執行，id 此時已有值
            runCatching {
                javaClass.declaredFields
                    .firstOrNull { it.name == "id" }
                    ?.also { it.isAccessible = true }
                    ?.get(this)?.toString() ?: StringUtil.EMPTY_STRING
            }.getOrDefault(StringUtil.EMPTY_STRING)
        }
    }
}
