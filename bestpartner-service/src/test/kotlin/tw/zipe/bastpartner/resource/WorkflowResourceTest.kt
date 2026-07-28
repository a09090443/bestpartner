package tw.zipe.bastpartner.resource

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.repository.WorkflowEdgeRepository
import tw.zipe.bastpartner.repository.WorkflowNodeRepository
import tw.zipe.bastpartner.repository.WorkflowRepository

/**
 * WorkflowResource REST 端點測試（@QuarkusTest + rest-assured）。
 *
 * 對齊 API #1-6（路徑前綴 /llm/workflow）與 RBAC：所有端點 @Authenticated，
 * 未登入應被拒（依 GlobalExceptionMapper，SecurityException 對應 403；
 * 部分情境亦可能為 401，故以「被拒」寬鬆斷言）。
 *
 * @author Gary
 * @created 2026/6/29
 */
@QuarkusTest
class WorkflowResourceTest {

    companion object {
        const val TEST_USER = "33333333-3333-3333-3333-333333333333"
    }

    @Inject
    lateinit var workflowRepository: WorkflowRepository

    @Inject
    lateinit var workflowNodeRepository: WorkflowNodeRepository

    @Inject
    lateinit var workflowEdgeRepository: WorkflowEdgeRepository

    @AfterEach
    fun cleanup() {
        workflowRepository.findByUserId(TEST_USER).forEach { wf ->
            wf.id?.let {
                workflowNodeRepository.deleteByWorkflowId(it)
                workflowEdgeRepository.deleteByWorkflowId(it)
            }
        }
        workflowRepository.deleteByUserId(TEST_USER)
    }

    /**
     * 案例 1：已登入建立 workflow，回 200 與 data.id
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = ["user"])
    fun testCreateReturnsId() {
        given().contentType(ContentType.JSON)
            .body("""{"name":"REST 建立測試","description":"demo"}""")
            .`when`().post("/llm/workflow/create")
            .then().statusCode(200)
            .body("code", equalTo(200))
            .body("data.id", notNullValue())
    }

    /**
     * 案例 2：未登入呼叫 list 應被拒（401 或 403）
     */
    @Test
    fun testListUnauthenticatedRejected() {
        given()
            .`when`().get("/llm/workflow/list")
            .then().statusCode(anyOf(equalTo(401), equalTo(403)))
    }

    /**
     * 案例 3：save 後 get 經 REST 無損 round-trip（2 node + 1 edge）
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = ["user"])
    fun testSaveThenGetRoundTrip() {
        val id = given().contentType(ContentType.JSON)
            .body(
                """
                {
                  "name":"REST RoundTrip",
                  "nodes":[
                    {"nodeKey":"a","type":"TRIGGER","positionX":0,"positionY":0,"config":{}},
                    {"nodeKey":"b","type":"TOOL","positionX":1,"positionY":1,"config":{}}
                  ],
                  "edges":[{"sourceNodeKey":"a","targetNodeKey":"b"}]
                }
                """.trimIndent()
            )
            .`when`().post("/llm/workflow/save")
            .then().statusCode(200)
            .body("data.id", notNullValue())
            .extract().path<String>("data.id")

        given().contentType(ContentType.JSON)
            .body("""{"id":"$id"}""")
            .`when`().post("/llm/workflow/get")
            .then().statusCode(200)
            .body("data.nodes.size()", equalTo(2))
            .body("data.edges.size()", equalTo(1))
    }

    /**
     * 案例 4：list 回當前使用者的 workflow 清單
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = ["user"])
    fun testListReturnsOwnWorkflows() {
        given().contentType(ContentType.JSON)
            .body("""{"name":"清單測試"}""")
            .`when`().post("/llm/workflow/create")
            .then().statusCode(200)

        given()
            .`when`().get("/llm/workflow/list")
            .then().statusCode(200)
            .body("code", equalTo(200))
            .body("data.size()", anyOf(equalTo(1), org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
    }

    /**
     * 案例 5：delete 已建立的 workflow 回 200
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = ["user"])
    fun testDeleteWorkflow() {
        val id = given().contentType(ContentType.JSON)
            .body("""{"name":"待刪除"}""")
            .`when`().post("/llm/workflow/create")
            .then().statusCode(200)
            .extract().path<String>("data.id")

        given().contentType(ContentType.JSON)
            .body("""{"id":"$id"}""")
            .`when`().post("/llm/workflow/delete")
            .then().statusCode(200)
            .body("code", equalTo(200))
    }

    /**
     * 案例 6（任務 #19）：已登入取得節點必填欄位清單，回 200 且 LLM_ASSISTANT 含 llmId、PROMPT 含 prompt
     */
    @Test
    @TestSecurity(user = TEST_USER, roles = ["user"])
    fun testNodeRequiredFields() {
        given()
            .`when`().get("/llm/workflow/nodeRequiredFields")
            .then().statusCode(200)
            .body("code", equalTo(200))
            .body("data.LLM_ASSISTANT", org.hamcrest.Matchers.hasItem("llmId"))
            .body("data.PROMPT", org.hamcrest.Matchers.hasItem("prompt"))
    }
}
