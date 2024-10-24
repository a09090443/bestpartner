package tw.zipe.bastpartner.tool

import dev.langchain4j.agent.tool.Tool
import tw.zipe.bastpartner.util.time.DateTimeUtils

/**
 * @author Gary
 * @created 2024/10/13
 */
class DateTool {

    @Tool("return current time")
    fun getCurrentTime() = DateTimeUtils.getDateNow(DateTimeUtils.dateTimeFormate1)
}
