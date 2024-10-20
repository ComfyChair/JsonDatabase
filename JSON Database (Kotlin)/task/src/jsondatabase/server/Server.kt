package jsondatabase.server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

private const val PORT = 23456
private const val ADDRESS = "127.0.0.1"
private const val ERROR = "ERROR"
private const val WELCOME_MSG ="Server started!"
private const val MAX_ENTRIES = 100

fun main() {
    try {
        ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS)).use { server ->
            println(WELCOME_MSG)
            try {
                server.accept().use { socket ->
                    DataInputStream(socket.getInputStream()).use { inStream ->
                        DataOutputStream(socket.getOutputStream()).use { outStream ->
                            val input = inStream.readUTF()
                            println("Received: $input")
                            val output = processRequest(input)
                            outStream.writeUTF(output)
                            println("Sent: $output")
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun processRequest(input: String) : String {
    val cmd = input.split("# ")
    if (cmd.size >= 2 && isIdxOk(cmd[1])) {
        val idx = cmd[1].toInt()
        return "A record # $idx was sent!"
    } else {
        return ERROR
    }
}

private fun isIdxOk(idx: String) : Boolean {
    val isDigits = idx.matches(Regex("[0-9]+"))
    val isInRange = idx.toInt() in 1..MAX_ENTRIES
    return isDigits && isInRange
}

