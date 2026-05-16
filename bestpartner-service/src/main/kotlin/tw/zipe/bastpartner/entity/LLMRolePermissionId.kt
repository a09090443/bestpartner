package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

/**
 * llm_role_permission 複合主鍵
 * @author Gary
 */
@Embeddable
data class LLMRolePermissionId(
    @Column(name = "role_num")
    var roleNum: Int = 0,

    @Column(name = "permission_num")
    var permissionNum: Int = 0
) : Serializable
