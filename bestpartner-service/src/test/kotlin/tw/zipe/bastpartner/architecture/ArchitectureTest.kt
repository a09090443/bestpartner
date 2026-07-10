package tw.zipe.bastpartner.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * 機械化強制（Harness 原則 3）：把 .claude/rules/ 中以文字描述的結構慣例，
 * 轉成 build 階段自動驗證的不變量，讓違規在 build 時就紅，而非靠人工 review 或 agent 自律。
 *
 * 紀律：所有規則皆「先通過現狀」。既有歷史漂移（見各規則註解）標為基線、刻意被規則容納，
 * 不在此次導入時強制修改已存在的類別。新增程式碼則必須遵循。
 *
 * 純 JVM bytecode 分析，非 @QuarkusTest，不需啟動容器。
 */
class ArchitectureTest {

    private val importedClasses: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages(BASE_PACKAGE)

    // ---------- 分層依賴方向：resource → service → repository → entity ----------

    @Test
    fun `service 不可依賴 resource 層`() {
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..resource..")
            .because("業務邏輯不得反向依賴 REST 控制器；service 應由 resource 呼叫，而非相反")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `repository 不可依賴 service 或 resource 層`() {
        noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..resource..")
            .because("資料存取層只負責持久化，不得依賴上層 service/resource")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `entity 不可依賴 service 或 resource 層`() {
        noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..resource..")
            .because("JPA 實體應為純資料模型，不得依賴 service/resource，以利重用與測試")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    // ---------- 命名與位置（對照 .claude/rules/naming-conventions.md）----------

    @Test
    fun `標註 @Path 的類別必須命名為 XxxResource`() {
        classes()
            .that().areAnnotatedWith("jakarta.ws.rs.Path")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Resource")
            .because("REST 控制器一律以 Resource 結尾，並放在 resource 套件（修正方式：重新命名為 XxxResource.kt）")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `repository 套件的類別必須命名為 XxxRepository`() {
        classes()
            .that().resideInAPackage("..repository..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Repository")
            .because("資料存取層命名一律以 Repository 結尾（修正方式：重新命名或移出 repository 套件）")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `service 套件的類別必須命名為 XxxService`() {
        // service.workflow 為執行引擎子套件（引擎/上下文/事件/executor 各有專屬命名），另由下一條規則約束
        classes()
            .that().resideInAPackage("..service..")
            .and().resideOutsideOfPackage("..service.workflow..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Service")
            .because("業務邏輯層命名一律以 Service 結尾（修正方式：重新命名或移出 service 套件）")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `workflow executor 套件的類別必須命名為 XxxExecutor`() {
        classes()
            .that().resideInAPackage("..service.workflow.executor..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Executor")
            .because("workflow 節點執行器一律以 Executor 結尾（修正方式：重新命名）")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `標註 @Entity 的類別必須放在 entity 套件`() {
        // 位置不變量：@Entity 一律落在 entity 套件。
        // 註：此處只強制「位置」，不強制「命名」——既有基線 LLMMcpUserSetting 帶 @Entity 卻無 Entity 後綴，
        // 仍合法通過；新增 entity 請遵循 XxxEntity.kt 命名。
        classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..entity..")
            .because("JPA 實體一律放在 entity 套件，便於 agent 定位資料模型")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    companion object {
        private const val BASE_PACKAGE = "tw.zipe.bastpartner"
    }
}
