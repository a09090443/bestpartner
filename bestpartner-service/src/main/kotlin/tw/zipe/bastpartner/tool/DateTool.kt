package tw.zipe.bastpartner.tool

import dev.langchain4j.agent.tool.Tool
import tw.zipe.bastpartner.util.time.DateTimeUtils

/**
 * @author Gary
 * @created 2024/10/13
 */
class DateTool {

    @Tool("以台灣時間為基準，會根據不同時區取得當地日期時間，格式是yyyy-MM-dd HH:mm:ss")
    fun getCurrentTime() = DateTimeUtils.getDateNow(DateTimeUtils.dateTimeFormate1)
}
