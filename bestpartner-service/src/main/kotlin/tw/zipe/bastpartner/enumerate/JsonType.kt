package tw.zipe.bastpartner.enumerate

/**
 * @author Gary
 * @created 2024/11/28
 */
enum class JsonType {
    OBJECT,      // {}
    ARRAY,       // []
    STRING,      // "value"
    NUMBER,      // 123 or 123.45
    BOOLEAN,     // true or false
    NULL         // null
}
