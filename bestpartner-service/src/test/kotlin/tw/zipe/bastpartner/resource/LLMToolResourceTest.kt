package tw.zipe.bastpartner.resource

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

/**
 * LLMToolResource REST 端點測試（@QuarkusTest + rest-assured）。
 *
 * 重點驗證 settingSchema 的 wire-format：專案同時掛 quarkus-rest-jackson 與
 * quarkus-rest-kotlin-serialization，settingSchema 為 kotlinx JsonObject，
 * 若 response writer 誤選 Jackson，JsonPrimitive 會被 bean 序列化成
 * {"content":"...","isString":true} 汙染契約，故必須在 HTTP 層斷言實際 JSON 結構。
 *
 * @author Gary
 * @created 2026/7/5
 */
@QuarkusTest
class LLMToolResourceTest {

    /**
     * /llm/tool/list 為公開端點（不需 token）。
     * 斷言 GoogleSearch 的 settingSchema.apiKey 以純 JSON 純量呈現：
     * type 為字串 "string"（而非 bean 物件）、required/sensitive 為布林。
     */
    @Test
    fun `list 回傳的 settingSchema 為結構化 JSON 而非 bean 序列化`() {
        given()
            .`when`().get("/llm/tool/list")
            .then().statusCode(200)
            .body("code", equalTo(200))
            .body(
                "data.find { it.name == 'GoogleSearch' }.settingSchema",
                notNullValue()
            )
            .body(
                "data.find { it.name == 'GoogleSearch' }.settingSchema.apiKey.type",
                equalTo("string")
            )
            .body(
                "data.find { it.name == 'GoogleSearch' }.settingSchema.apiKey.required",
                equalTo(true)
            )
            .body(
                "data.find { it.name == 'GoogleSearch' }.settingSchema.apiKey.sensitive",
                equalTo(true)
            )
    }
}
