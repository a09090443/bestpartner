package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tw.zipe.bastpartner.enumerate.VectorStore
import tw.zipe.bastpartner.model.VectorStoreModel

/**
 * @author Gary
 * @created 2024/10/19
 */
@Entity
@Table(name = "vector_store_setting")
class VectorStoreSettingEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 使用者
     */
    @Column(name = "account", nullable = false)
    var account: String = StringUtil.EMPTY_STRING

    /**
     * 向量資料庫類型
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: VectorStore = VectorStore.MILVUS

    /**
     * 自定義別名
     */
    @Column(name = "alias", nullable = true)
    var alias: String = StringUtil.EMPTY_STRING

    /**
     * 向量資料庫設定
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vector_setting", columnDefinition = "json", nullable = true)
    lateinit var vectorSetting: VectorStoreModel

}
