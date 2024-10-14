package tw.zipe.basepartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/10/12
 */
@Entity
@Table(name = "llm_model")
class LlmModel {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    var id: Long? = null

    /**
     * 類型
     */
    @Column(name = "type", nullable = false)
    var type: String? = null

    /**
     * 模型名稱
     */
    @Column(name = "model",nullable = true)
    var model: String? = null

    /**
     * 平台
     */
    @Column(name = "provider",nullable = true)
    var provider: String? = null

    /**
     * 平台 API Key
     */
    @Column(name = "api_key",nullable = true)
    var apiKey: String? = null

    /**
     * 平台 URL
     */
    @Column(name = "base_url",nullable = true)
    var baseUrl: String? = null

    /**
     * 別名
     */
    @Column(name = "name",nullable = true)
    var name: String? = null

    /**
     * 響應長度
     */
    @Column(name = "response_limit",nullable = true)
    var responseLimit: String? = null

    /**
     * 溫度
     */
    @Column(name = "temperature",nullable = true)
    var temperature: String? = null

    /**
     * TOP_P
     */
    @Column(name = "top_p",nullable = true)
    var topP: String? = null

    /**
     * 模型維度
     */
    @Column(name = "dimension",nullable = true)
    var dimension: Int? = null
}
