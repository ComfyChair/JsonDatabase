package jsondatabase.client

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

private const val PORT = 23456
private const val ADDRESS = "127.0.0.1"

fun main(args: Array<String>) {
    val request: String = composeRequest(args)
    try {
        Socket(InetAddress.getByName(ADDRESS), PORT).use { socket ->
            DataInputStream(socket.getInputStream()).use { input ->
                DataOutputStream(socket.getOutputStream()).use { output ->
                    println("Client started!")
                    output.writeUTF(request)
                    println("Sent: $request")
                    if (request != "exit") {
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

fun composeRequest(args: Array<String>): String {
    val returnString = StringBuilder()
    for (i in 1..args.lastIndex step 2) {
        returnString.append("${args[i]} ")
    }
    return returnString.toString().trim()
}
