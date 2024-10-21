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
    private var content : MutableMap<String, JsonElement>
    private val file: File = path.toFile()
        .also {
            if (it.exists()) {
                content = Json.decodeFromString<MutableMap<String, JsonElement>>(it.readText())
                println("\nRead database file: in ${it.absolutePath}:\n$content\n")
            } else {
                if (!it.parentFile.exists()) {
                    it.parentFile.mkdirs()
                }
                it.createNewFile()
                content = mutableMapOf()
                it.writeText(Json.encodeToString(content))
            }
        }

    fun get(requestKey: JsonElement): Response {
        val keys = parseKeys(requestKey)
        if (keys.size < 2) {
            val value = content[keys.first()]
            return if (value != null) {
                Response(OK, value)
            } else {
                Response(ERROR, reason =  NO_SUCH_KEY)
            }
        } else {
            val parent : JsonObject? = traverseTo(keys.dropLast(1))
            val value : JsonElement? = parent?.get(keys.last())
            return if (value != null) {
                Response(OK, value)
            } else {
                Response(ERROR, reason =  NO_SUCH_KEY)
            }
        }
    }

    fun set(requestKey: JsonElement, requestValue: JsonElement) : Response {
        val keys = parseKeys(requestKey)
        updateContent(keys, requestValue)
        file.writeText(Json.encodeToString(content))
        return Response(OK)
    }

    fun delete(requestKey: JsonElement) : Response {
        val keys = parseKeys(requestKey)
        val keyQueue = LinkedList(keys)
        // traverse down
        var parent = JsonObject(content)
        var child: JsonElement? = content[keyQueue.pop()]
        while (keyQueue.isNotEmpty() && child is JsonObject) {
            parent = child
            child = parent[keyQueue.pop()]
        }
        if (keyQueue.isEmpty()) {
            // key present, we can proceed
            val newParent = parent.toMutableMap()
            newParent.remove(keys.last())
            updateContent(keys.dropLast(1), JsonObject(newParent))
            file.writeText(Json.encodeToString(content))
            return Response(OK)
        } else {
            // key not present
            return Response(ERROR, reason =  NO_SUCH_KEY)
        }
    }

    private fun traverseTo(keys: List<String>): JsonObject? {
        var currentNode : JsonElement? = content[keys[0]]
        for (i in 1 until keys.size) {
            if (currentNode is JsonPrimitive) {
                return null
            } else {
                currentNode = (currentNode as JsonObject)[keys[i]]
            }
        }
        return (currentNode as JsonObject?)
    }
    private fun updateContent(inKeys: List<String>, inValue: JsonElement) {
        // traverse tree until first missing key or JsonPrimitive
        //TODO: simplify or restructure?
        val keyQueue : LinkedList<String> = LinkedList(inKeys)
        var childKey: String = keyQueue.poll()
        var parentElement: Map<String, JsonElement> = content
        var childElement : JsonElement? = content[childKey]
        val keyStack = Stack<String>()
        val parentStack = Stack<JsonObject>()
        keyStack.push(childKey)
        // traverse down, looking for existing keys
        while (childElement != null && childElement is JsonObject && keyQueue.isNotEmpty()) {
            childKey = keyQueue.poll()
            parentElement = childElement
            childElement = childElement[childKey]
            keyStack.push(childKey)
            parentStack.push(parentElement)
        }
        //traverse up, updating values
        var insertionElement : JsonElement = inValue
        while (keyStack.size > 1) {
            val parentMap = parentStack.pop().toMutableMap()
            parentMap[keyStack.pop()] = insertionElement
            insertionElement = JsonObject(parentMap)
        }
        content[keyStack.pop()] = insertionElement
    }

    @Serializable
    data class Response(val response: String,
                        val value: JsonElement = JsonPrimitive(""),
                        val reason: String = "")

    companion object {
        private fun parseKeys(keyElement: JsonElement): List<String> {
            return if (keyElement is JsonArray) {
                keyElement.map { item -> item.toString().trim('"') }
            } else {
                listOf(keyElement.toString().trim('"'))
            }
        }
    }
}

