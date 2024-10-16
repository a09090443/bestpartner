package tw.zipe.basepartner.entity

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
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
