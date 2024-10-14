package tw.zipe.basepartner.tool

import dev.langchain4j.agent.tool.Tool
import tw.zipe.basepartner.DateTimeUtils

/**
 * @author Gary
 * @created 2024/10/13
 */
class DateTool {

    @Tool("return current time")
    fun getCurrentTime() = DateTimeUtils.getDateNow(DateTimeUtils.dateTimeFormate1)
}
