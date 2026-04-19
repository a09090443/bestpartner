package tw.zipe.bastpartner.service

import dev.langchain4j.skills.DefaultSkill
import dev.langchain4j.skills.DefaultSkillResource
import dev.langchain4j.skills.Skills
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.constant.GLOBAL_SKILL_DIR
import tw.zipe.bastpartner.dto.SkillDTO
import tw.zipe.bastpartner.dto.SkillResourceDTO
import tw.zipe.bastpartner.entity.LLMSkillEntity
import tw.zipe.bastpartner.entity.LLMSkillResourceEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.SkillScope
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.form.SkillUploadForm
import tw.zipe.bastpartner.repository.LLMSkillRepository
import tw.zipe.bastpartner.repository.LLMSkillResourceRepository

/**
 * @author Gary
 * @created 2025/04/18
 */
@ApplicationScoped
class SkillService(
    private val skillRepository: LLMSkillRepository,
    private val skillResourceRepository: LLMSkillResourceRepository,
    private val securityValidator: SecurityValidator,
    private val systemService: SystemService,
    @ConfigProperty(name = "file.upload.dir") private val fileUploadDir: String
) {

    private val logger: Logger = Logger.getLogger(SkillService::class.java)

    private val validSkillNamePattern = Regex("^[\\w\\-]+$")

    @Transactional
    fun uploadSkill(form: SkillUploadForm): SkillDTO {
        val userId = securityValidator.validateLoggedInUser()
        val skillName = extractSkillName(form)
        val skillDir = resolveSkillDir(SkillScope.USER, userId, skillName)
        return doUpload(form, userId, skillName, skillDir, SkillScope.USER)
    }

    @Transactional
    fun uploadGlobalSkill(form: SkillUploadForm): SkillDTO {
        val userId = securityValidator.validateLoggedInUser()
        val skillName = extractSkillName(form)
        val skillDir = resolveSkillDir(SkillScope.GLOBAL, userId, skillName)
        return doUpload(form, userId, skillName, skillDir, SkillScope.GLOBAL)
    }

    @Transactional
    fun deleteSkill(id: String) {
        val userId = securityValidator.validateLoggedInUser()
        val entity = findOwnedSkill(id, userId)
        removeSkillFiles(entity)
        skillResourceRepository.findBySkillId(id).forEach { skillResourceRepository.delete(it) }
        skillRepository.deleteById(id)
    }

    @Transactional
    fun deleteGlobalSkill(id: String) {
        val entity = skillRepository.findById(id) ?: throw ServiceException(AppMessage.SKILL_NOT_FOUND)
        if (entity.scope != SkillScope.GLOBAL) throw ServiceException(AppMessage.SKILL_NOT_FOUND)
        removeSkillFiles(entity)
        skillResourceRepository.findBySkillId(id).forEach { skillResourceRepository.delete(it) }
        skillRepository.deleteById(id)
    }

    fun getSkill(id: String): SkillDTO {
        val userId = securityValidator.validateLoggedInUser()
        val entity = findReadableSkill(id, userId)
        val mainContent = resolveSkillContent(entity)

        val resources = entity.dirPath?.let { dirPath ->
            skillResourceRepository.findBySkillId(id).mapNotNull { res ->
                val file = Path.of(dirPath, res.relativePath).toFile()
                if (!file.exists()) {
                    logger.warn("Resource 檔案找不到: ${res.relativePath}")
                    return@mapNotNull null
                }
                SkillResourceDTO(id = res.id, skillId = res.skillId, relativePath = res.relativePath, content = file.readText(Charsets.UTF_8))
            }
        } ?: skillResourceRepository.findBySkillId(id).map { res ->
            SkillResourceDTO(id = res.id, skillId = res.skillId, relativePath = res.relativePath, content = res.content)
        }

        return SkillDTO(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            content = mainContent,
            dirPath = entity.dirPath,
            isGlobal = entity.scope == SkillScope.GLOBAL,
            resources = resources
        )
    }

    fun getSkills(): List<SkillDTO> {
        val userId = securityValidator.validateLoggedInUser()
        val globalSkills = skillRepository.findAllByScope(SkillScope.GLOBAL).map { it.toDTO() }
        val userSkills = skillRepository.findAllByUserId(userId).map { it.toDTO() }
        return globalSkills + userSkills
    }

    fun buildSkills(skillIds: List<String>): Skills {
        val globalEntities = skillRepository.findAllByScope(SkillScope.GLOBAL)
        val userEntities = skillIds.mapNotNull { skillRepository.findById(it) }
        val allEntities = (globalEntities + userEntities).distinctBy { it.id }

        val skillList = allEntities.mapNotNull { entity ->
            val content = resolveSkillContent(entity) ?: run {
                logger.warn("Skill [${entity.name}] 無可用內容，略過")
                return@mapNotNull null
            }
            val resources = resolveSkillResources(entity)
            val builder = DefaultSkill.builder()
                .name(entity.name)
                .description(entity.description.takeIf { !it.isNullOrBlank() } ?: entity.name)
                .content(content)
            if (resources.isNotEmpty()) builder.resources(resources)
            builder.build()
        }
        return Skills.from(skillList)
    }

    private fun doUpload(form: SkillUploadForm, userId: String, skillName: String, skillDir: Path, scope: SkillScope): SkillDTO {
        val existingEntity = when (scope) {
            SkillScope.GLOBAL -> skillRepository.findByScopeAndName(SkillScope.GLOBAL, skillName)
            SkillScope.USER -> skillRepository.findByUserIdAndName(userId, skillName)
        }

        existingEntity?.let { skillDir.toFile().takeIf { it.exists() }?.deleteRecursively() }

        extractZipSafely(form.file!!.uploadedFile(), skillDir)

        val skillMdFile = skillDir.resolve("skill.md").toFile()
        if (!skillMdFile.exists()) {
            skillDir.toFile().deleteRecursively()
            throw ServiceException(AppMessage.SKILL_MD_NOT_FOUND)
        }

        val dirPathStr = skillDir.toAbsolutePath().toString()
        val entity = existingEntity ?: LLMSkillEntity().apply { this.userId = userId }
        entity.name = skillName
        entity.description = form.description ?: entity.description
        entity.dirPath = dirPathStr
        entity.scope = scope
        entity.content = null
        skillRepository.saveOrUpdate(entity)

        val skillId = entity.id!!
        skillResourceRepository.findBySkillId(skillId).forEach { skillResourceRepository.delete(it) }

        skillDir.toFile().walk()
            .filter { it.isFile && it.name != "skill.md" }
            .forEach { file ->
                LLMSkillResourceEntity().apply {
                    this.skillId = skillId
                    this.relativePath = skillDir.relativize(file.toPath()).toString()
                    this.content = null
                    skillResourceRepository.saveOrUpdate(this)
                }
            }

        return SkillDTO(id = entity.id, name = entity.name, description = entity.description, dirPath = dirPathStr, isGlobal = scope == SkillScope.GLOBAL)
    }

    private fun extractSkillName(form: SkillUploadForm): String {
        val uploadedFile = form.file ?: throw ServiceException(AppMessage.SKILL_ZIP_NOT_PROVIDED)
        val originalName = uploadedFile.fileName()
        if (!originalName.lowercase().endsWith(".zip")) throw ServiceException(AppMessage.SKILL_ZIP_FORMAT_INVALID)
        val skillName = originalName.removeSuffix(".zip").removeSuffix(".ZIP")
        if (skillName.isBlank() || !validSkillNamePattern.matches(skillName)) throw ServiceException(AppMessage.SKILL_ZIP_FORMAT_INVALID)
        return skillName
    }

    private fun resolveSkillDir(scope: SkillScope, userId: String, skillName: String): Path = when (scope) {
        SkillScope.GLOBAL -> {
            val customDir = systemService.getSystemSettingValue(GLOBAL_SKILL_DIR)
            if (!customDir.isNullOrBlank()) Path.of(customDir, skillName)
            else Path.of(fileUploadDir, "skills", "global", skillName)
        }
        SkillScope.USER -> Path.of(fileUploadDir, "skills", userId, skillName)
    }

    private fun removeSkillFiles(entity: LLMSkillEntity) {
        entity.dirPath?.let { File(it).takeIf { f -> f.exists() }?.deleteRecursively() }
    }

    private fun resolveSkillContent(entity: LLMSkillEntity): String? {
        entity.dirPath?.let { dirPath ->
            val skillMd = Path.of(dirPath, "skill.md").toFile()
            if (skillMd.exists()) return skillMd.readText(Charsets.UTF_8)
            logger.warn("Skill [${entity.name}] dirPath 存在但 skill.md 找不到: $dirPath")
            return null
        }
        return entity.content?.takeIf { it.isNotBlank() }
    }

    private fun resolveSkillResources(entity: LLMSkillEntity): List<DefaultSkillResource> {
        val dirPath = entity.dirPath ?: return emptyList()
        return skillResourceRepository.findBySkillId(entity.id!!)
            .mapNotNull { res ->
                val file = Path.of(dirPath, res.relativePath).toFile()
                if (!file.exists()) {
                    logger.warn("Resource 檔案找不到: ${res.relativePath}")
                    return@mapNotNull null
                }
                DefaultSkillResource.builder()
                    .relativePath(res.relativePath)
                    .content(file.readText(Charsets.UTF_8))
                    .build()
            }
    }

    private fun extractZipSafely(zipPath: Path, targetDir: Path) {
        targetDir.toFile().mkdirs()
        val canonicalTarget = targetDir.toFile().canonicalPath

        ZipInputStream(Files.newInputStream(zipPath)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryFile = targetDir.resolve(entry.name).toFile()
                val canonicalEntry = entryFile.canonicalPath

                if (!canonicalEntry.startsWith(canonicalTarget + File.separator) && canonicalEntry != canonicalTarget) {
                    throw ServiceException(AppMessage.SKILL_ZIP_PATH_TRAVERSAL)
                }

                if (entry.isDirectory) entryFile.mkdirs()
                else {
                    entryFile.parentFile?.mkdirs()
                    Files.copy(zis, entryFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun findOwnedSkill(id: String, userId: String): LLMSkillEntity {
        val entity = skillRepository.findById(id) ?: throw ServiceException(AppMessage.SKILL_NOT_FOUND)
        if (entity.userId != userId) throw ServiceException(AppMessage.SKILL_NOT_FOUND)
        return entity
    }

    private fun findReadableSkill(id: String, userId: String): LLMSkillEntity {
        val entity = skillRepository.findById(id) ?: throw ServiceException(AppMessage.SKILL_NOT_FOUND)
        if (entity.scope != SkillScope.GLOBAL && entity.userId != userId) throw ServiceException(AppMessage.SKILL_NOT_FOUND)
        return entity
    }

    private fun LLMSkillEntity.toDTO() = SkillDTO(
        id = id,
        name = name,
        description = description,
        dirPath = dirPath,
        isGlobal = scope == SkillScope.GLOBAL
    )
}
