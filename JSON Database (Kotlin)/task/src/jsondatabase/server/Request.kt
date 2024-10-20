package jsondatabase.server


class Request(reqString: String) {
    enum class RequestType { EXIT, SET, GET, DELETE, UNKNOWN; }

    var requestType: RequestType = RequestType.UNKNOWN
    var cell : Int? = null
    var content : String = ""
    init {
        val cmd = reqString.split(" ", limit = 3)
        when {
            reqString.uppercase() == RequestType.EXIT.name -> init(RequestType.EXIT)
            cmd.size < 2 || isKeywordUnknown(cmd[0]) || isInvalidIdx(cmd[1]) -> init(RequestType.UNKNOWN)
            cmd[0].uppercase() == RequestType.GET.name -> init(RequestType.GET, cmd[1])
            cmd[0].uppercase() == RequestType.DELETE.name -> init(RequestType.DELETE, cmd[1])
            cmd[0].uppercase() == RequestType.SET.name -> {
                if (cmd.size < 3) init(RequestType.UNKNOWN)
                else init(RequestType.SET, cmd[1], cmd[2])
            }
        }
    }

    private fun init(requestType: RequestType, vararg args: String) {
        this.requestType = requestType
        when (requestType) {
            RequestType.GET, RequestType.DELETE -> cell = args[0].toInt()
            RequestType.SET -> {
                cell = args[0].toInt()
                content = args[1]
            }
            else -> {}
        }
    }

    private fun isKeywordUnknown(cmdString: String): Boolean {
        return cmdString.uppercase() !in RequestType.values().map {it.name}
    }

    private fun isInvalidIdx(cmdIdx: String): Boolean {
        val isAllDigits = cmdIdx.all { it.isDigit() }
        val isInRange = cmdIdx.toInt() in 1..MAX_ENTRIES
        return !isAllDigits || !isInRange
    }
}