package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import tw.zipe.bastpartner.converter.PasswordEncryptConverter
import tw.zipe.bastpartner.enumerate.VectorStore

/**
 * @author Gary
 * @created 2024/10/19
 */
@Entity
@Table(name = "vector_store_setting")
class VectorStoreSettingEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "user_id", nullable = false)
    var userId: String = StringUtil.EMPTY_STRING

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: VectorStore? = null

    @Column(name = "alias", nullable = true)
    var alias: String = StringUtil.EMPTY_STRING

    @Column(name = "url")
    var url: String? = null

    @Column(name = "username")
    var username: String? = null

    @Column(name = "password")
    @Convert(converter = PasswordEncryptConverter::class)
    var password: String? = null

    @Column(name = "collection_name")
    var collectionName: String? = null

    @Column(name = "dimension")
    var dimension: Int? = null

    @Column(name = "request_log")
    var requestLog: Boolean = false

    @Column(name = "response_log")
    var responseLog: Boolean = false
}
