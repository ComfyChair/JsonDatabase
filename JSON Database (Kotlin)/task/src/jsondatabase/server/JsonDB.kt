package jsondatabase.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Path
import java.util.*

const val NO_SUCH_KEY = "No such key"
private const val ERROR = "ERROR"
private const val OK = "OK"

class JsonDB(path: Path) {
    // values can be Strings or nested MutableMaps, therefore declared as Any
    // -> should be encoded as JSON primitives or JSON Objects
    @Serializable
    private lateinit var content : MutableMap<String, JsonElement>
    private val file: File = path.toFile()
        .also {
            if (it.exists()) {
                println("Reading file")
                content = Json.decodeFromString<MutableMap<String, JsonElement>>(it.readText())
            } else {
                println("Creating file in ${it.absolutePath}")
                if (!it.parentFile.exists()) {
                    it.parentFile.mkdirs()
                }
                it.createNewFile()
                it.writeText(Json.encodeToString(mutableMapOf<String, Any>()))
            }
        }

    fun get(requestKey: JsonElement): Response {
        val keys = parseKeys(requestKey)
        val parent = traverseTo(keys.dropLast(1))
        val value : JsonElement? = (parent as? JsonObject)?.get(keys.last())
        return if (value != null) {
            Response(OK, Json.encodeToString(parent))
        } else {
            Response(ERROR, reason =  NO_SUCH_KEY)
        }
    }

    fun set(requestKey: JsonElement, requestValue: JsonElement) : Response {
        val keys = parseKeys(requestKey)
        println("Called set with key: $keys, value: $requestValue")
        updateContent(keys, requestValue)
        file.writeText(Json.encodeToString(content))
        return Response(OK)
    }

    fun delete(requestKey: JsonElement) : Response {
        val keys = parseKeys(requestKey)
        var secondToLast: Any? = content
        for (key in keys.dropLast(1)) {
            if (secondToLast != null && secondToLast is Map<*, *>) {
                secondToLast = secondToLast[key]
            } else {
                secondToLast = null
                break
            }
        }
        val removed : Any? = (secondToLast as? MutableMap<*, *>)?.remove(keys.last())
        if (removed != null) {
            file.writeText(Json.encodeToString(content))
            return Response(OK)
        } else {
            return Response(ERROR, reason =  NO_SUCH_KEY)
        }
    }

    private fun traverseTo(keys: List<String>): JsonElement? {
        var currentNode : JsonElement? = content[keys[0]]
        for (i in 1 until keys.size - 1) {
            if (currentNode is JsonPrimitive) {
                currentNode = null
                break
            } else {
                currentNode = (currentNode as JsonObject)[keys[i]]
            }
        }
        return currentNode
    }
    private fun updateContent(inKeys: List<String>, inValue: JsonElement) {
        // traverse tree until first missing key or JsonPrimitive
        val keyQueue : LinkedList<String> = LinkedList(inKeys)
        var rootKey: String = keyQueue.poll()
        var rootElement : JsonElement? = content[rootKey]
        while (rootElement != null && rootElement is JsonObject && keyQueue.isNotEmpty()) {
            rootKey = keyQueue.poll()
            rootElement = (rootElement)[rootKey]
        }
        // traverse remaining keys backwards, creating JsonElements
        var insertionElement : JsonElement = inValue
        for (key in keyQueue.asReversed()) {
            insertionElement = JsonObject(mapOf(key to insertionElement))
        }
        content[rootKey] = insertionElement
    }

    @Serializable
    data class Response(val response: String, val value: String = "", val reason: String = "")

    companion object {
        private fun parseKeys(keyElement: JsonElement): List<String> {
            return if (keyElement is JsonArray) {
                keyElement.map { item -> item.toString() }
            } else {
                listOf(keyElement.toString())
            }
        }
        private fun parseValue(valueString: String): JsonElement {
            val value : JsonElement = if (valueString.startsWith("{")) {
                Json.decodeFromString<JsonElement>(valueString)
            } else {
                JsonPrimitive(valueString)
            }
            println(value)
            return value
        }
    }
}

