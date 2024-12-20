package tw.zipe.bastpartner.util

import kotlin.reflect.full.memberProperties
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import tw.zipe.bastpartner.exception.ValidationException

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun throwIfInvalid() {
        if (!isValid) throw ValidationException(errors)
    }
}

interface ValidationRule {
    fun validate(value: Any?): Boolean
    fun getErrorMessage(fieldName: String): String
}

class EmailFormatRule : ValidationRule {
    private val emailRegex = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    )

    override fun validate(value: Any?): Boolean {
        if (value == null || value.toString().isBlank()) return true
        return value is String && value.matches(emailRegex)
    }

    override fun getErrorMessage(fieldName: String): String {
        return "欄位 $fieldName 的 email 格式不正確"
    }
}

class NotEmptyRule : ValidationRule {
    override fun validate(value: Any?): Boolean {
        return when (value) {
            null -> false
            is String -> value.isNotBlank()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            is Array<*> -> value.isNotEmpty()
            else -> true
        }
    }

    override fun getErrorMessage(fieldName: String): String {
        return "欄位 $fieldName 不可為空值"
    }
}

class JsonFormatRule(private val strictMode: Boolean = true) : ValidationRule {
    override fun validate(value: Any?): Boolean {
        if (value == null || value.toString().isBlank()) return true

        return try {
            val jsonElement = Json.parseToJsonElement(value.toString())

            // 嚴格模式下，只允許複雜的 JSON 結構
            if (strictMode) {
                when {
                    jsonElement.isJsonObject -> true  // 只接受 Object
                    jsonElement.isJsonArray -> true   // 只接受 Array
                    else -> false                     // 拒絕基本類型
                }
            } else {
                true  // 非嚴格模式下接受所有有效 JSON
            }
        } catch (e: SerializationException) {
            false
        }
    }

    override fun getErrorMessage(fieldName: String): String {
        return "欄位 $fieldName 的 JSON 格式不正確（需要是 JSON 物件或陣列）"
    }

    // 擴充方法：檢查是否為 JSON 物件
    private val JsonElement.isJsonObject: Boolean
        get() = this is JsonObject

    // 擴充方法：檢查是否為 JSON 陣列
    private val JsonElement.isJsonArray: Boolean
        get() = this is JsonArray
}

class DTOValidator {
    private val fieldRules = mutableMapOf<String, MutableList<ValidationRule>>()
    private val nestedValidators = mutableMapOf<String, (Any) -> ValidationResult>()

    fun <T : Any> validate(dto: T): ValidationResult {
        val errors = mutableListOf<String>()

        val properties = dto::class.memberProperties

        // 驗證一般欄位規則
        fieldRules.forEach { (fieldName, rules) ->
            val property = properties.find { it.name == fieldName }
            if (property != null) {
                val value = property.getter.call(dto)
                rules.forEach { rule ->
                    if (!rule.validate(value)) {
                        errors.add(rule.getErrorMessage(fieldName))
                    }
                }
            } else {
                errors.add("找不到欄位: $fieldName")
            }
        }

        // 驗證巢狀物件
        nestedValidators.forEach { (fieldPath, validator) ->
            val pathParts = fieldPath.split(".")
            var currentObject: Any? = dto

            // 遍歷物件路徑
            for (part in pathParts) {
                val property = currentObject?.let {
                    it::class.memberProperties.find { prop -> prop.name == part }
                }
                currentObject = property?.getter?.call(currentObject)

                if (currentObject == null) {
                    errors.add("找不到巢狀欄位: $fieldPath")
                    return@forEach
                }
            }

            // 驗證找到的巢狀物件
            currentObject?.let {
                val nestedResult = validator(it)
                if (!nestedResult.isValid) {
                    errors.addAll(nestedResult.errors.map { error -> "$fieldPath: $error" })
                }
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    fun <T : Any> validateOrThrow(dto: T): T {
        validate(dto).throwIfInvalid()
        return dto
    }

    class Builder<T : Any>(private val dto: T) {
        private val validator = DTOValidator()
        private var throwOnInvalid = false

        fun requireNotEmpty(vararg fields: String): Builder<T> {
            fields.forEach { field ->
                if (!validator.fieldRules.containsKey(field)) {
                    validator.fieldRules[field] = mutableListOf()
                }
                validator.fieldRules[field]?.add(NotEmptyRule())
            }
            return this
        }

        fun validateEmail(vararg fields: String): Builder<T> {
            fields.forEach { field ->
                if (!validator.fieldRules.containsKey(field)) {
                    validator.fieldRules[field] = mutableListOf()
                }
                validator.fieldRules[field]?.add(EmailFormatRule())
            }
            return this
        }

        fun validateJson(vararg fields: String, strictMode: Boolean = true): Builder<T> {
            fields.forEach { field ->
                if (!validator.fieldRules.containsKey(field)) {
                    validator.fieldRules[field] = mutableListOf()
                }
                validator.fieldRules[field]?.add(JsonFormatRule(strictMode))
            }
            return this
        }

        /**
         * 驗證巢狀物件
         * @param fieldPath 欄位路徑，例如 "address" 或 "company.address"
         * @param validationBlock 巢狀物件的驗證規則
         */
        fun validateNested(fieldPath: String, validationBlock: Builder<Any>.() -> Unit): Builder<T> {
            validator.nestedValidators[fieldPath] = { nestedObject ->
                validate(nestedObject, validationBlock)
            }
            return this
        }

        fun throwOnInvalid(): Builder<T> {
            throwOnInvalid = true
            return this
        }

        fun validate(): ValidationResult {
            val result = validator.validate(dto)
            if (throwOnInvalid) {
                result.throwIfInvalid()
            }
            return result
        }
    }

    companion object {
        fun <T : Any> validate(dto: T, init: Builder<T>.() -> Unit): ValidationResult {
            return Builder(dto).apply(init).validate()
        }

        fun <T : Any> validateOrThrow(dto: T, init: Builder<T>.() -> Unit): T {
            validate(dto, init).throwIfInvalid()
            return dto
        }
    }
}

// 範例使用
data class Address(
    val street: String?,
    val city: String?,
    val zipCode: String?
)

data class Company(
    val name: String?,
    val email: String?,
    val address: Address?
)

data class JsonData(
    val jsonField1: String?,
    val jsonField2: String?
)

data class UserDTO(
    val id: Long?,
    val name: String?,
    val email: String?,
    val company: Company?,
    val homeAddress: Address?,
    val jsonData: JsonData?
)

fun main() {
    // 建立測試資料
    val userDTO = UserDTO(
        id = 1,
        name = "John",
        email = "invalid.email",
        company = Company(
            name = "",  // 空字串
            email = "company@example.com",
            address = Address(
                street = "  ",  // 空白字串
                city = "New York",
                zipCode = null
            )
        ),
        homeAddress = Address(
            street = "123 Main St",
            city = null,
            zipCode = ""  // 空字串
        ),
        jsonData = JsonData(
            jsonField1 = """{"name": "John", "age": 30}""",
            jsonField2 = """[1, 2, 3]"""  // 非物件或陣列
        )
    )

    try {
        // 驗證整個物件結構
        DTOValidator.validateOrThrow(userDTO) {
            // 驗證頂層欄位
            requireNotEmpty("name", "email")
            validateEmail("email")

            // 驗證公司資訊
            validateNested("company") {
                requireNotEmpty("name", "email")
                validateEmail("email")

                // 驗證公司地址
                validateNested("address") {
                    requireNotEmpty("street", "city", "zipCode")
                }
            }

            // 驗證住家地址
            validateNested("homeAddress") {
                requireNotEmpty("street", "city", "zipCode")
            }
            // 驗證Jsone格式
            validateNested("jsonData") {
                validateJson("jsonField1", "jsonField2")
            }

        }
    } catch (e: ValidationException) {
        println("驗證錯誤：")
        println(e.message)
    }
}
