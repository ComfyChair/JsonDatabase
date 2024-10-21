package jsondatabase.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.file.FileSystems
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

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
    private val relativePath = FileSystems.getDefault().getPath("server", "data", "db.json")
    private val database = JsonDB(relativePath)
    private val executor = Executors.newFixedThreadPool(4)
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock: Lock = lock.readLock()
    private val writeLock: Lock = lock.writeLock()

    private fun processRequest(requestString: String) : String? {
        val request = Json.decodeFromString<Request>(requestString)
        val response : Future<JsonDB.Response> = when (request.type) {
            RequestType.exit -> return null
            RequestType.get -> executor.submit (
                Callable {
                    synchronized(readLock) {
                        database.get(request.key)
                    }
                }
            )
            RequestType.set -> executor.submit(
                Callable {
                    synchronized(writeLock) {
                        database.set(request.key, request.value)
                    }
                }
            )
            RequestType.delete -> executor.submit(
                Callable {
                    synchronized(writeLock) {
                        database.delete(request.key)
                    }
                }
            )
        }
        println(response.get())
        return Json.encodeToString(response.get())
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
    data class Request(val type: RequestType, val key: JsonElement, val value: JsonElement = JsonPrimitive(""))
}

