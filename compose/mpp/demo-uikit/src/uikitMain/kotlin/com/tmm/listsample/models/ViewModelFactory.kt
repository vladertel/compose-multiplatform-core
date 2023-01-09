package com.tmm.listsample.models

import com.tmm.listsample.models.impl.JSONCompositionModel
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIScreen

private const val TMMFloatMax: platform.CoreGraphics.CGFloat = 10000.0


private fun createModelsFromJSON(json: Map<*, *>?): List<IBaseViewModel> {
    if (json == null) {
        return listOf()
    }

    val result = mutableListOf<IBaseViewModel>()
    val sections = json["json"] as? List<Map<*, *>>;
    sections?.forEach { section ->
        val blocks = section["blocks"] as? List<Map<*, *>>;
        blocks?.forEach {
            val typedBlock = it as? Map<*, *>
            val model = createModelWithBlock(typedBlock as Map<String, Any>)
            if (model != null) {
                result.add(model)
            }
        }
    }
    return result
}

private fun createModelWithBlock(block: Map<String, *>?): IBaseViewModel? {
    if (block == null) {
        return null
    }

    val model = when (block["blockStyleType"] as? String) {
        "composition" -> JSONCompositionModel(block)
        else -> {
            null
        }
    }
    
    return model
}

internal fun fetchCompositionModels(useJSON: Boolean, callback: (List<IBaseViewModel>) -> Unit) {
    callback(
        (1..100).map {
            object : ICompositionModel {
                override val title: String?
                    get() = "Title"
                override val subtitle: String?
                    get() = "Subtitle"
                override val overlyTopLeft = fakeItem()
                override val overlyView1= fakeItem()
                override val overlyView2= fakeItem()
                override val overlyView3= fakeItem()
                override val overlyTopRight= fakeItem()
                override val label: ICompositionItem = fakeItem()

                override val blockId: String = it.toString()
                override val reportInfo: Map<String, Any>?
                    get() = TODO("Not yet implemented")
                override val operations: Map<String, Any>?
                    get() = TODO("Not yet implemented")
                override val flipInfos: Map<String, Any>?
                    get() = TODO("Not yet implemented")
                override val extraData: Map<String, Any>?
                    get() = TODO("Not yet implemented")
                override val data: Map<String, Any>?
                    get() = TODO("Not yet implemented")
            }
        }
    )
}

private fun fakeItem() = object : ICompositionItem {
    override val bgColor: String? = null
    override val radius: String? = "3.4"
    override val alpha: String? = "0.5"
    override val shadowColor: String? = null
    override val textColor: String? = null
    override val text: String? = "text"

}