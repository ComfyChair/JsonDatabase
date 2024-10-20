package jsondatabase.server

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException

private const val PORT = 23456
private const val ADDRESS = "127.0.0.1"
private const val WELCOME_MSG ="Server started!"

fun main() {
    try {
        val server = ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS))
        println(WELCOME_MSG)
        while (!server.isClosed) {
            val session = Server.Session(server.accept(), server)
            session.start()
        }
    } catch (e: SocketException) {
        println(e.message)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

object Server {
        private val database = JsonDB()

        private fun processRequest(requestString: String) : String? {
            val request = Json.decodeFromString<Request>(requestString)
            return when (request.type) {
                RequestType.get -> Json.encodeToString(database.get(request.key))
                RequestType.set -> Json.encodeToString(database.set(request.key, request.value))
                RequestType.delete -> Json.encodeToString(database.delete(request.key))
                RequestType.exit -> null
            }
        }

    class Session(private val client: Socket, private val server: ServerSocket) : Thread() {
        override fun run() {
            try {
                DataInputStream(client.getInputStream()).use { inStream ->
                    DataOutputStream(client.getOutputStream()).use { outStream ->
                        while (client.isConnected) {
                            if (inStream.available() > 0) {
                                val input = inStream.readUTF()
                                //println("Received: $input")
                                val output = processRequest(input)
                                if (output == null) {
                                    break
                                } else {
                                    outStream.writeUTF(output)
                                    //println("Sent: $output")
                                }
                            }
                        }
                        client.close()
                        server.close() // for test purposes
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    // enum values in lowercase because of test requirements
    enum class RequestType {get, set, delete, exit}
    @Serializable
    class Request(val type: RequestType, val key: String ="", val value: String = "")
}

