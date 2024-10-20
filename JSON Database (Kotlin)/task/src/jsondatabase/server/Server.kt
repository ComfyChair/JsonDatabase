package jsondatabase.server

import jsondatabase.server.Request.RequestType
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.net.InetAddress
import java.net.ServerSocket

private const val PORT = 23456
private const val ADDRESS = "127.0.0.1"
private const val WELCOME_MSG ="Server started!"
private const val ERROR = "ERROR"
private const val OK = "OK"

const val MAX_ENTRIES = 1000
// initializes database with empty Strings
private val database: Array<String> = Array(MAX_ENTRIES) { "" }

fun main() {
    try {
        val server = ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS))
        println(WELCOME_MSG)
        while (!server.isClosed) {
            val session = Session(server.accept(), server)
            session.start()
        }
    } catch (e: IOException) {
        e.printStackTrace()
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
private fun processRequest(requestString: String) : String? {
    val request = Request(requestString)
    when (request.requestType) {
        RequestType.UNKNOWN -> return ERROR
        RequestType.GET -> {
            return if (request.cell == null || database[request.cell!!] == "") {
                ERROR
            } else {
                database[request.cell!!]
            }
        }
        RequestType.SET -> {
            database[request.cell!!] = request.content
            return OK
        }
        RequestType.DELETE -> {
            database[request.cell!!] = ""
            return OK
        }
        RequestType.EXIT -> return null
    }
}
