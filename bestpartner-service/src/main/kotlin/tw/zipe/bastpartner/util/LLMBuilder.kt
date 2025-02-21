package tw.zipe.bastpartner.util

import com.fasterxml.jackson.databind.ObjectMapper
import tw.zipe.bastpartner.dto.LLMSettingDTO
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.model.LLModel

open class LLMBuilder {

    open fun build(llmSetting: LLMSettingDTO, type: ModelType): Any {
        val objectMapper = ObjectMapper()

        val platform = Platform.getPlatform(llmSetting.platformName)
        val llmModel = objectMapper.readValue(llmSetting.modelSetting, LLModel::class.java)
        return build(platform, llmModel, type)
    }

    open fun build(platform: Platform, llmModel: LLModel, type: ModelType): Any {
        return when (type) {
            ModelType.EMBEDDING -> platform.getLLMBean().embeddingModel(llmModel)
            ModelType.CHAT -> platform.getLLMBean().chatModel(llmModel)
            ModelType.STREAMING_CHAT -> platform.getLLMBean().chatModelStreaming(llmModel)
        }
    }
}
