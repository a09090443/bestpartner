package tw.zipe.bastpartner.tool

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import java.time.ZoneId
import java.time.ZonedDateTime
import tw.zipe.bastpartner.util.time.DateTimeUtils

/**
 * @author Gary
 * @created 2024/10/13
 */
class DateTool {

    @Tool("以台灣時間為基準，會根據不同時區取得當地日期時間，格式是yyyy-MM-dd HH:mm:ss")
    fun getCurrentTime(@P("根據國家或城市回應正確的當地時間") timeZone: String): String {
        // 直接從指定時區獲取當前時間
        val zonedDateTime = ZonedDateTime.now(ZoneId.of(timeZone))
        // 格式化時間
        return zonedDateTime.format(DateTimeUtils.dateTimeFormate1)
    }
}
