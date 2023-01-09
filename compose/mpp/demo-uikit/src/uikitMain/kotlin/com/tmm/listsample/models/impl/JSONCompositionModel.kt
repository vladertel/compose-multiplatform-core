package com.tmm.listsample.models.impl

import com.tmm.listsample.models.ICompositionItem
import com.tmm.listsample.models.ICompositionModel

private value class JSONCompositionItem(private val nativeModel: Map<String, Any>) :
    ICompositionItem {
    override val bgColor: String? get() = nativeModel["bgColor"] as String?
    override val radius: String? get() = nativeModel["radius"] as String?
    override val alpha: String? get() = nativeModel["alpha"] as String?
    override val shadowColor: String? get() = nativeModel["shadowColor"] as String?
    override val textColor: String? get() = nativeModel["textColor"] as String?
    override val text: String? get() = nativeModel["text"] as String?
}

private fun safeItem(json: Map<String, *>, key: String): JSONCompositionItem? {
    val data = json["data"]
    if (data is Map<*, *>) {
        return JSONCompositionItem(data[key] as Map<String, Any>)
    }
    return null
}

internal value class JSONCompositionModel(private val json: Map<String, *>) :
    ICompositionModel {

    override val blockId: String? get() = json["blockId"] as String?

    override val title: String? get() = json["title"] as String?

    override val subtitle: String? get() = json["subtitle"] as String?

    override val overlyTopLeft: ICompositionItem? get() = safeItem(json, "overlyTopleft")

    override val overlyView1: ICompositionItem? get() = safeItem(json, "overlyView1")
    override val overlyView2: ICompositionItem? get() = safeItem(json, "overlyView2")
    override val overlyView3: ICompositionItem? get() = safeItem(json, "overlyView3")
    override val overlyTopRight: ICompositionItem? get() = safeItem(json, "overlyTopRight")
    override val label: ICompositionItem? get() = safeItem(json, "label")

    override val reportInfo: Map<String, Any>? get() = json["reportInfo"] as Map<String, Any>?
    override val operations: Map<String, Any>? get() = json["operations"] as Map<String, Any>?

    override val flipInfos: Map<String, Any>? get() = json["flipInfos"] as Map<String, Any>?

    override val extraData: Map<String, Any>? get() = json["extraData"] as Map<String, Any>?

    override val data: Map<String, Any>? get() = json["data"] as Map<String, Any>?
}