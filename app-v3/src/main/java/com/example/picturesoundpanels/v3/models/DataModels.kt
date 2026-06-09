package com.example.picturesoundpanels.v3.models

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Card(
    var imageUri: String = "",
    var label: String = "",
    var audioPath: String = "",
    var imageScale: Float = 1f,
    var imageOffsetX: Float = 0f,
    var imageOffsetY: Float = 0f
) {
    fun hasImage(): Boolean = imageUri.isNotEmpty()
    fun hasAudio(): Boolean = audioPath.isNotEmpty()

    fun clear() {
        imageUri = ""
        label = ""
        audioPath = ""
        resetImagePosition()
    }

    fun resetImagePosition() {
        imageScale = 1f
        imageOffsetX = 0f
        imageOffsetY = 0f
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("imageUri", imageUri)
            put("label", label)
            put("audioPath", audioPath)
            put("imageScale", imageScale.toDouble())
            put("imageOffsetX", imageOffsetX.toDouble())
            put("imageOffsetY", imageOffsetY.toDouble())
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): Card {
            return Card(
                imageUri = obj.optString("imageUri", ""),
                label = obj.optString("label", ""),
                audioPath = obj.optString("audioPath", ""),
                imageScale = obj.optDouble("imageScale", 1.0).toFloat(),
                imageOffsetX = obj.optDouble("imageOffsetX", 0.0).toFloat(),
                imageOffsetY = obj.optDouble("imageOffsetY", 0.0).toFloat()
            )
        }
    }
}

data class Panel(
    var icon: String,
    var name: String,
    val cards: MutableList<Card> = MutableList(4) { Card() }
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("icon", icon)
            put("name", name)
            put("cards", JSONArray().apply {
                cards.forEach { put(it.toJson()) }
            })
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): Panel {
            val panel = Panel(
                icon = obj.optString("icon", "⭐"),
                name = obj.optString("name", "Panel")
            )
            val cardArray = obj.optJSONArray("cards")
            if (cardArray != null) {
                panel.cards.clear()
                for (i in 0 until cardArray.length().coerceAtMost(4)) {
                    panel.cards.add(Card.fromJson(cardArray.getJSONObject(i)))
                }
            }
            while (panel.cards.size < 4) {
                panel.cards.add(Card())
            }
            return panel
        }
    }
}
