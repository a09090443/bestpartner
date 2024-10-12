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
@Table(name = "AIGC_MODEL")
class AigcModel {
    @Id
    @Column(name = "ID", nullable = false)
    var id: Long? = null

    @Column(name = "TYPE", nullable = false)
    var type: String? = null

    @Column(name = "MODEL",nullable = true)
    var model: String? = null

    @Column(name = "PROVIDER",nullable = true)
    var provider: String? = null

    @Column(name = "NAME",nullable = true)
    var name: String? = null

    @Column(name = "RESPONSE_LIMIT",nullable = true)
    var responseLimit: String? = null

    @Column(name = "TEMPERATURE",nullable = true)
    var temperature: String? = null

    @Column(name = "TOP_P",nullable = true)
    var topP: String? = null

    @Column(name = "API_KEY",nullable = true)
    var apiKey: String? = null

    @Column(name = "BASE_URL",nullable = true)
    var baseUrl: String? = null

    @Column(name = "SECRET_KEY",nullable = true)
    var secretKey: String? = null

    @Column(name = "ENDPOINT",nullable = true)
    var endpoint: String? = null

    @Column(name = "DIMENSIONS",nullable = true)
    var dimensions: Int? = null
}
