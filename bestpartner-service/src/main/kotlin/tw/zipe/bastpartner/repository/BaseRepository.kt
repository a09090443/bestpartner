package tw.zipe.bastpartner.repository

import io.netty.util.internal.StringUtil
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.security.identity.SecurityIdentity
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.Tuple
import jakarta.transaction.Transactional
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.time.LocalDateTime
import tw.zipe.bastpartner.entity.BaseEntity
import tw.zipe.bastpartner.exception.ServiceException

/**
 * 基/礎資料庫操作的抽象類別，提供通用的 CRUD 操作和進階查詢功能
 *
 * 特點：
 * 1. 支援自動審計欄位填充（創建時間、更新時間、創建者、更新者）
 * 2. 提供原生 SQL 查詢的安全執行器
 * 3. 支援單一結果和列表結果的查詢
 * 4. 內建型別轉換和錯誤處理機制
 *
 * @author Gary
 * @created 2024/10/28
 */
abstract class BaseRepository<T : Any, ID : Any> : PanacheRepositoryBase<T, ID> {

    @Inject
    protected var em: EntityManager? = null

    @Inject
    protected var identity: SecurityIdentity? = null

    private val entityClass: Class<T> = initEntityClass()

    /**
     * 初始化實體類別型別
     * 通過反射獲取泛型參數的實際類型
     */
    @Suppress("UNCHECKED_CAST")
    private fun initEntityClass(): Class<T> {
        var superClass = javaClass.superclass
        while (superClass != null) {
            if (superClass.genericSuperclass is ParameterizedType) {
                val parameterizedType = superClass.genericSuperclass as ParameterizedType
                return parameterizedType.actualTypeArguments[0] as Class<T>
            }
            superClass = superClass.superclass
        }
        throw IllegalStateException("Could not find generic type parameter")
    }

    /**
     * 獲取當前使用者名稱
     * 如果未登入則返回空字串
     */
    protected fun getCurrentUsername(): String {
        return identity?.principal?.name ?: StringUtil.EMPTY_STRING
    }

    /**
     * 設置審計欄位（創建時間、更新時間、創建者、更新者）
     * @param entity 要設置的實體
     * @param isNew 是否為新建實體
     */
    private fun setAuditFields(entity: Any, isNew: Boolean = true) {
        if (entity is BaseEntity) {
            val now = LocalDateTime.now()
            val username = getCurrentUsername()

            if (isNew) {
                entity.createdAt = now
                entity.createdBy = username
            } else {
                entity.updatedAt = now
                entity.updatedBy = username
            }
        }
    }

    @Transactional
    override fun persist(entity: T) {
        setAuditFields(entity)
        super.persist(entity)
    }

    @Transactional
    override fun persist(entities: Iterable<T>) {
        entities.forEach { setAuditFields(it) }
        super.persist(entities)
    }

    /**
     * 安全的 SQL 執行器，提供型別安全的查詢和參數綁定
     *
     * 特點：
     * 1. 支援命名參數和位置參數
     * 2. 自動處理審計欄位
     * 3. 提供型別安全的結果映射
     * 4. 內建錯誤處理機制
     */
    protected inner class SafeSqlExecutor<T>(
        private val entityClass: Class<T>
    ) {
        private var sql: String = ""
        private val params: MutableList<Any> = mutableListOf()
        private val paramMap: MutableMap<String, Any> = mutableMapOf()
        private var isInsert: Boolean = false
        private var isUpdate: Boolean = false

        /**
         * 設置 SQL 查詢語句
         * @param sqlString SQL 查詢字串
         * @return SafeSqlExecutor 實例，支援鏈式調用
         */
        fun withSql(sqlString: String): SafeSqlExecutor<T> {
            this.sql = sqlString
            val normalizedSql = sqlString.trim().uppercase()
            isInsert = normalizedSql.startsWith("INSERT")
            isUpdate = normalizedSql.startsWith("UPDATE")
            return this
        }

        /**
         * 設置位置參數
         * @param parameters 參數值列表
         * @return SafeSqlExecutor 實例
         */
        fun withParams(vararg parameters: Any): SafeSqlExecutor<T> {
            params.addAll(parameters)
            return this
        }

        /**
         * 設置命名參數
         * @param params 參數名稱和值的映射
         * @return SafeSqlExecutor 實例
         */
        fun withParamMap(params: Map<String, Any>): SafeSqlExecutor<T> {
            this.paramMap.putAll(params)
            return this
        }

        /**
         * 處理審計欄位的 SQL 修改
         * 對於 INSERT 和 UPDATE 語句自動添加審計欄位
         */
        private fun processAuditFields(): String {
            val username = getCurrentUsername()
            val now = LocalDateTime.now()

            return when {
                isInsert -> {
                    sql.replace(
                        ")",
                        ", created_at, updated_at, created_by, updated_by)"
                    ).replace(
                        "VALUES (",
                        "VALUES ("
                    ) + ", '$now', '$now', '$username', '$username'"
                }
                isUpdate -> {
                    if (!sql.lowercase().contains("updated_at")) {
                        sql = sql.replace(
                            "WHERE",
                            ", updated_at = '$now', updated_by = '$username' WHERE"
                        )
                    }
                    sql
                }
                else -> sql
            }
        }

        /**
         * 執行更新操作（INSERT、UPDATE、DELETE）
         * @return 受影響的行數
         */
        fun executeUpdate(): Int {
            val processedSql = processAuditFields()
            val query = em!!.createNativeQuery(processedSql)
            paramMap.forEach { (key, value) -> query.setParameter(key, value) }
            params.forEachIndexed { index, param -> query.setParameter(index + 1, param) }
            return query.executeUpdate()
        }

        /**
         * 執行查詢並映射結果到指定類型
         * @param resultClass 目標類型的 Class 對象
         * @return 查詢結果列表
         */
        @Suppress("UNCHECKED_CAST")
        fun <R : Any> select(resultClass: Class<R>): List<R> {
            val processedSql = processAuditFields()
            val query = em!!.createNativeQuery(processedSql, Tuple::class.java)

            // 設置參數
            paramMap.forEach { (key, value) -> query.setParameter(key, value) }
            params.forEachIndexed { index, param -> query.setParameter(index + 1, param) }

            return try {
                (query.resultList as? List<Tuple>)?.map { tuple ->
                    mapTupleToClass(tuple, resultClass)
                } ?: emptyList()
            } catch (e: ClassCastException) {
                println("Error casting query result: ${e.message}")
                emptyList()
            }
        }

        /**
         * 查詢單一結果
         * @param resultClass 目標類型的 Class 對象
         * @return 單一結果或 null
         */
        fun <R : Any> selectOne(resultClass: Class<R>): R? {
            return try {
                select(resultClass).firstOrNull()
            } catch (e: NoResultException) {
                null
            }
        }

        /**
         * 查詢單一結果，如果不存在則拋出異常
         * @param resultClass 目標類型的 Class 對象
         * @return 單一結果
         * @throws NoResultException 當查詢結果為空時拋出
         */
        fun <R : Any> selectOneOrThrow(resultClass: Class<R>): R {
            return selectOne(resultClass) ?: throw NoResultException("No result found for query")
        }

        /**
         * 將查詢結果 Tuple 映射到指定類型的實例
         * @param tuple 查詢結果 Tuple
         * @param clazz 目標類型的 Class 對象
         * @return 映射後的實例
         */
        private fun <R : Any> mapTupleToClass(tuple: Tuple, clazz: Class<R>): R {
            val instance = clazz.getDeclaredConstructor().newInstance()
            val fields = clazz.declaredFields.associateBy { it.name.lowercase() }

            tuple.elements.forEach { tupleElement ->
                val columnAlias = tupleElement.alias.lowercase()

                if (columnAlias.contains(".")) {
                    // Handle nested object mapping
                    val (parentField, childField) = columnAlias.split(".")
                    val parentInstance = fields[parentField]?.let { field ->
                        field.isAccessible = true
                        if (field[instance] == null) {
                            // Create new instance of nested object if null
                            val nestedClass = field.type
                            val nestedInstance = nestedClass.getDeclaredConstructor().newInstance()
                            field.set(instance, nestedInstance)
                        }
                        field[instance]
                    }

                    // Set nested field value
                    parentInstance?.let { parent ->
                        val nestedFields = parent.javaClass.declaredFields.associateBy { it.name.lowercase() }
                        val properChildFieldName = childField.replace("_", "").lowercase()
                        val nestedField = nestedFields[properChildFieldName]?.also {
                            it.isAccessible = true
                        } ?: throw IllegalStateException("No such field: $childField in ${parent.javaClass.simpleName}")

                        setFieldValue(nestedField, parent, tuple[tupleElement.alias])
                    }
                } else {
                    // Handle flat object mapping
                    val properFieldName = columnAlias.replace("_", "").lowercase()
                    fields[properFieldName]?.let { field ->
                        field.isAccessible = true
                        setFieldValue(field, instance, tuple[tupleElement.alias])
                    }
                }
            }

            return instance
        }

        /**
         * 設置欄位值，處理不同型別的轉換
         * @param field 目標欄位
         * @param instance 目標實例
         * @param value 要設置的值
         */
        private fun setFieldValue(field: Field, instance: Any, value: Any?) {
            if (value == null) return

            field.isAccessible = true
            try {
                when (field.type) {
                    String::class.java -> field.set(instance, value.toString())
                    Int::class.java, Integer::class.java -> field.set(instance, value.toString().toIntOrNull() ?: 0)
                    Long::class.java -> field.set(instance, value.toString().toLongOrNull() ?: 0L)
                    Double::class.java -> field.set(instance, value.toString().toDoubleOrNull() ?: 0.0)
                    Float::class.java -> field.set(instance, value.toString().toFloatOrNull() ?: 0.0f)
                    Boolean::class.java -> field.set(instance, value.toString().toBoolean())
                    LocalDateTime::class.java -> when (value) {
                        is LocalDateTime -> field.set(instance, value)
                        is java.sql.Timestamp -> field.set(instance, value.toLocalDateTime())
                        else -> println("Unsupported datetime type: ${value::class.java}")
                    }
                    else -> field.set(instance, value)
                }
            } catch (e: Exception) {
                println("Error setting field ${field.name}: ${e.message}")
            }
        }

        /**
         * 使用自定義映射器轉換查詢結果
         * @param mapper 自定義映射函數
         * @return 映射後的結果列表
         */
        @Suppress("UNCHECKED_CAST")
        fun <R> getResultList(mapper: (Array<Any>) -> R): List<R> {
            val processedSql = processAuditFields()
            val query = em!!.createNativeQuery(processedSql)
            params.forEachIndexed { index, param -> query.setParameter(index + 1, param) }

            return try {
                (query.resultList as? List<Array<Any>>)?.map(mapper) ?: emptyList()
            } catch (e: ClassCastException) {
                println("Error casting result list: ${e.message}")
                emptyList()
            }
        }
    }

    @Transactional
    protected fun executeUpdateWithTransaction(executor: SafeSqlExecutor<*>): Int {
        return executor.executeUpdate()
    }

    protected fun createSqlExecutor(): SafeSqlExecutor<T> {
        return SafeSqlExecutor(entityClass)
    }

    protected fun setAuditFields(entities: Collection<Any>, isNew: Boolean = true) {
        entities.forEach { entity -> setAuditFields(entity, isNew) }
    }

    protected fun <R : Any> executeSelectOne(
        sql: String,
        params: Map<String, Any>,
        resultClass: Class<R>
    ): R? {
        return createSqlExecutor()
            .withSql(sql)
            .withParamMap(params)
            .selectOne(resultClass)
    }

    protected fun <R : Any> executeSelectOne(
        sql: String,
        params: List<Any>,
        resultClass: Class<R>
    ): R? {
        return createSqlExecutor()
            .withSql(sql)
            .withParams(*params.toTypedArray())
            .selectOne(resultClass)
    }

    protected fun <R : Any> executeSelectOneOrThrow(
        sql: String,
        params: Map<String, Any>,
        resultClass: Class<R>
    ): R {
        return executeSelectOne(sql, params, resultClass)
            ?: throw NoResultException("No result found for query")
    }

    protected fun <R : Any> executeSelect(
        sql: String,
        params: Map<String, Any>,
        resultClass: Class<R>
    ): List<R> {
        return createSqlExecutor()
            .withSql(sql)
            .withParamMap(params)
            .select(resultClass)
    }

    protected fun <R : Any> executeSelect(
        sql: String,
        params: List<Any>,
        resultClass: Class<R>
    ): List<R> {
        return createSqlExecutor()
            .withSql(sql)
            .withParams(*params.toTypedArray())
            .select(resultClass)
    }

    @Transactional
    open fun saveOrUpdate(entity: T): T {
        persist(entity)
        return entity
    }

    open fun findOptionalById(id: ID): T? {
        return findById(id)
    }

    open fun findAllEntities(): List<T> {
        return listAll()
    }

    @Transactional
    open fun deleteEntities(entities: List<T>) {
        delete("id in ?1", entities.map { it })
    }

    @Transactional
    open fun saveEntities(entities: List<T>): List<T> {
        persist(entities)
        return entities
    }

    open fun countAll(): Long {
        return count()
    }

    open fun existsById(id: ID): Boolean {
        return count("id = ?1", id) > 0
    }

    protected fun initParamsMap(vararg params: Pair<String, Any>): Map<String, Any> {
        val userName = identity?.principal?.name?.takeIf { it.isNotEmpty() } ?: throw ServiceException("請確認已登入")
        val defaultParams = mapOf(
            "updatedAt" to LocalDateTime.now(),
            "updatedBy" to userName
        )
        return defaultParams + params.toMap()
    }
    /**
     * 使用範例：
     *
     * 1. 基本查詢：
     * ```
     * // 使用命名參數查詢
     * val user = executeSelectOneOrThrow(
     *     "SELECT * FROM users WHERE id = :id",
     *     mapOf("id" to 1),
     *     User::class.java
     * )
     *
     * // 使用位置參數查詢列表
     * val users = executeSelect(
     *     "SELECT * FROM users WHERE age > ?",
     *     listOf(18),
     *     User::class.java
     * )
     * ```
     *
     * 2. 更新操作：
     * ```
     * val affected = createSqlExecutor()
     *     .withSql("UPDATE users SET name = :name WHERE id = :id")
     *     .withParamMap(mapOf(
     *         "name" to "New Name",
     *         "id" to 1
     *     ))
     *     .executeUpdate()
     * ```
     *
     * 3. 複雜查詢：
     * ```
     * val results = createSqlExecutor()
     *     .withSql("""
     *         SELECT u.*, d.name as dept_name
     *         FROM users u
     *         JOIN departments d ON u.dept_id = d.id
     *         WHERE u.age > :age
     *     """)
     *     .withParamMap(mapOf("age" to 20))
     *     .select(UserWithDept::class.java)
     * ```
     */
}
