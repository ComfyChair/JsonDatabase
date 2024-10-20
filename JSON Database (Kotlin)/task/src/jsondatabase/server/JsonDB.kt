package jsondatabase.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path

const val NO_SUCH_KEY = "No such key"
private const val ERROR = "ERROR"
private const val OK = "OK"

class JsonDB(path: Path) {
    @Serializable
    private var content : MutableMap<String, String> = mutableMapOf()
    val file: File = path.toFile()
        .also {
            if (it.exists()) {
                println("Reading file")
                content = Json.decodeFromString(it.readText())
            } else {
                println("Creating file in ${it.absolutePath}")
                if (!it.parentFile.exists()) {
                    it.parentFile.mkdirs()
                }
                it.createNewFile()
                it.writeText(Json.encodeToString(content))
            }
        }

    fun get(query: String): Response {
        val value = content[query]
        return if (value != null) Response(OK, value) else Response(ERROR, reason =  NO_SUCH_KEY)
    }

    fun set(query: String, value: String) : Response {
        if (content[query] != value) {
            content[query] = value
            file.writeText(Json.encodeToString(content))
        }
        return Response(OK)
    }

    fun delete(query: String) : Response {
        val removed = content.remove(query)
        if (removed != null) {
            file.writeText(Json.encodeToString(content))
            return Response(OK)
        } else {
            return Response(ERROR, reason =  NO_SUCH_KEY)
        }
    }
    @Serializable
    data class Response(val response: String, val value: String = "", val reason: String = "")
}

