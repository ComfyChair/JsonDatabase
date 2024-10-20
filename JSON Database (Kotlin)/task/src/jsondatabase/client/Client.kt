package jsondatabase.client

import jsondatabase.server.Server
import jsondatabase.server.Server.RequestType
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.readText

private const val PORT = 23456
private const val ADDRESS = "127.0.0.1"
private val PATH = Path("src", "jsondatabase", "client", "data")

fun main(args: Array<String>) {
    val request: Server.Request = composeRequest(args)
    val jsonRequest = Json.encodeToString(request)
    try {
        Socket(InetAddress.getByName(ADDRESS), PORT).use { socket ->
            DataInputStream(socket.getInputStream()).use { input ->
                DataOutputStream(socket.getOutputStream()).use { output ->
                    println("Client started!")
                    output.writeUTF(jsonRequest)
                    println("Sent: $jsonRequest")
                    if (request.type != RequestType.exit) {
                        val reply = input.readUTF()
                        println("Received: $reply")
                    }
                }
            }
        }
    }
    catch (e: IOException) {
        e.printStackTrace()
    }
}

fun composeRequest(args: Array<String>): Server.Request {
    var type = ""
    var key = ""
    var value = ""
    for (i in 0..args.lastIndex step 2) {
        when (args[i]) {
            "-t" -> type = args[i + 1]
            "-k" -> key = args[i + 1]
            "-v" -> value = args[i + 1]
            "-in" -> {
                val completePath = PATH.resolve(args[i + 1])
                val command = completePath.absolute().readText()
                return Json.decodeFromString(Server.Request.serializer(), command)
            }
            else -> {}
        }
    }
    val request = Server.Request(RequestType.valueOf(type.lowercase()), key, value)
    return request
}
