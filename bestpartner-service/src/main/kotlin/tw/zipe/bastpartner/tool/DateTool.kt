package tw.zipe.bastpartner.tool

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import java.time.ZoneId
import tw.zipe.bastpartner.util.time.DateTimeUtils

/**
 * @author Gary
 * @created 2024/10/13
 */
class DateTool {

    @Tool("""
        以台灣時間為基準，會根據不同時區取得當地日期時間，timeZone的輸入格式為"國家/城市"，如:Australia/Darwin, Asia/Taipei, Africa/Harare
        """)
    fun getCurrentTime(@P("根據國家或城市回應正確的當地時間") timeZone: String): String {
        // 格式化時間
        return DateTimeUtils.getFormattedDateTime(zoneId = ZoneId.of(timeZone))
    }
}
