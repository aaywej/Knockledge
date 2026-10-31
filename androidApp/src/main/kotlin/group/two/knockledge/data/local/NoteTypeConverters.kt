package group.two.knockledge.data.local

import androidx.room.TypeConverter
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Sentence
import org.json.JSONArray
import org.json.JSONObject

/** Stores the list-valued note fields as JSON text columns. */
class NoteTypeConverters {
    /** Encodes sentences as a JSON array in the field order [jsonToSentences] reads back. */
    @TypeConverter
    fun sentencesToJson(list: List<Sentence>?): String? = list?.let { l ->
        JSONArray(l.map {
            JSONObject()
                .put("speaker", it.speaker)
                .put("startTimeMs", it.startTimeMs)
                .put("endTimeMs", it.endTimeMs)
                .put("text", it.text)
                .put("confidence", it.confidence.toDouble())
        }).toString()
    }

    /** Parses the output of [sentencesToJson]; null input stays null. */
    @TypeConverter
    fun jsonToSentences(json: String?): List<Sentence>? = json?.let { s ->
        val a = JSONArray(s)
        List(a.length()) {
            val o = a.getJSONObject(it)
            Sentence(
                o.getString("speaker"),
                o.getLong("startTimeMs"),
                o.getLong("endTimeMs"),
                o.getString("text"),
                o.getDouble("confidence").toFloat(),
            )
        }
    }

    /** Encodes chapter markers and their bullet lists as a JSON array. */
    @TypeConverter
    fun markersToJson(list: List<ChapterMarker>?): String? = list?.let { l ->
        JSONArray(l.map {
            JSONObject()
                .put("startTimeMs", it.startTimeMs)
                .put("title", it.title)
                .put("keyBullets", JSONArray(it.keyBullets))
        }).toString()
    }

    /** Parses the output of [markersToJson]; null input stays null. */
    @TypeConverter
    fun jsonToMarkers(json: String?): List<ChapterMarker>? = json?.let { s ->
        val a = JSONArray(s)
        List(a.length()) {
            val o = a.getJSONObject(it)
            ChapterMarker(o.getLong("startTimeMs"), o.getString("title"), strings(o.getJSONArray("keyBullets")))
        }
    }

    /** Encodes a string list as a JSON array of strings. */
    @TypeConverter
    fun stringsToJson(list: List<String>?): String? = list?.let { JSONArray(it).toString() }

    /** Parses the output of [stringsToJson]; null input stays null. */
    @TypeConverter
    fun jsonToStrings(json: String?): List<String>? = json?.let { strings(JSONArray(it)) }

    private fun strings(a: JSONArray) = List(a.length()) { a.getString(it) }
}
