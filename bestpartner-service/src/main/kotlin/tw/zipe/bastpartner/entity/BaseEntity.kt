package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
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

}
