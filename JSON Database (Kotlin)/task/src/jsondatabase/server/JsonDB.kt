package jsondatabase.server

import kotlinx.serialization.Serializable

const val NO_SUCH_KEY = "No such key"
private const val ERROR = "ERROR"
private const val OK = "OK"

class JsonDB {
    private val content : MutableMap<String, String> = mutableMapOf()

    fun get(query: String): Response {
        val value = content[query]
        return if (value != null) Response(OK, value) else Response(ERROR, reason =  NO_SUCH_KEY)
    }

    fun set(query: String, value: String) : Response {
        content[query] = value
        return Response(OK)
    }

    fun delete(query: String) : Response {
        val removed = content.remove(query)
        return if (removed!=null) Response(OK) else Response(ERROR, reason =  NO_SUCH_KEY)
    }
    @Serializable
    data class Response(val response: String, val value: String = "", val reason: String = "")
}

