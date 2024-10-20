package jsondatabase.client

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

private const val PORT = 23456
private const val ADDRESS = "127.0.0.1"

fun main() {
    try {
        Socket(InetAddress.getByName(ADDRESS), PORT).use { socket ->
            DataInputStream(socket.getInputStream()).use { input ->
                DataOutputStream(socket.getOutputStream()).use { output ->
                    println("Client started!")
                    val msg = "Give me a record # 12"
                    output.writeUTF(msg)
                    println("Sent: $msg")
                    val reply = input.readUTF()
                    println("Received: $reply")
                }
            }
        }
    }
    catch (e: IOException) {
        e.printStackTrace()
    }
}
