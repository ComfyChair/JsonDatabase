package jsondatabase.server

private const val ERROR = "ERROR"
private const val OK = "OK"
private const val EXIT = "exit"
private const val MAX_ENTRIES = 100
enum class Commands {SET, GET, DELETE}

private val database: Array<String> = Array(MAX_ENTRIES) { "" }

fun main() {
    var input = readln()
    while (input.lowercase() != EXIT) {
        val cmd = input.split(" ")
        // TODO: only split off first two words, the rest is all supposed to go into the database
        if (cmd.size < 2 // check if the command has enough arguments
            || cmd[0].uppercase() !in Commands.values().map { it.name } // check if command keyword is known
            || cmd[1].any { !it.isDigit() } // check if second argument is a number
            || cmd[1].toInt() > MAX_ENTRIES // check if number is too large
            ) {
            println(ERROR)
        } else {
            val cell = cmd[1].toInt()
            when (cmd[0].uppercase()) {
                "SET" -> {
                    if (cmd.size < 3) println(ERROR)
                    else database[cell] = cmd[2] .also { println(OK) }
                }
                "GET" -> {
                    if (database[cell].isBlank()) println(ERROR)
                    else println(database[cell]) .also { println(OK) }
                }
                "DELETE" -> database[cell] = "" .also { println(OK) }
            }
        }
        input = readln()
    }
}