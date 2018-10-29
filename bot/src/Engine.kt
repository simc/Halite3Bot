import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList

class Command private constructor(val command: String) {
    companion object {
        fun spawnShip(): Command {
            return Command("g")
        }

        fun transformShipIntoDropoffSite(id: Int): Command {
            return Command("c $id")
        }

        fun move(id: Int, direction: Direction): Command {
            return Command("m $id ${direction.charValue}")
        }
    }
}

class Input(line: String?) {
    private val input: List<String> = line?.split(" ") ?: ArrayList()
    private var current: Int = 0

    val nextInt: Int
        get() = Integer.parseInt(input[current++])

    companion object {

        fun readInput(): Input {
            return Input(readLine())
        }

        fun readLine(): String? {
            return kotlin.io.readLine()
        }
    }
}

class Log private constructor(private val file: FileWriter) {

    private class AtExit : Thread() {
        override fun run() {
            if (INSTANCE != null) {
                return
            }

            val nowInNanos = System.nanoTime()
            val filename = "bot-unknown-$nowInNanos.log"
            try {
                FileWriter(filename).use { writer ->
                    for (message in LOG_BUFFER) {
                        writer.append(message).append('\n')
                    }
                }
            } catch (e: IOException) {
                // Nothing much we can do here.
            }

        }
    }

    companion object {

        private var INSTANCE: Log? = null
        private val LOG_BUFFER = ArrayList<String>()

        init {
            Runtime.getRuntime().addShutdownHook(AtExit())
        }

        internal fun open(botId: Int) {
            if (INSTANCE != null) {
                Log.log("Error: log: tried to open($botId) but we have already opened before.")
                throw IllegalStateException()
            }

            val filename = "bot-$botId.log"
            val writer: FileWriter
            try {
                writer = FileWriter(filename)
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }

            INSTANCE = Log(writer)

            try {
                for (message in LOG_BUFFER) {
                    writer.append(message).append('\n')
                }
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }

            LOG_BUFFER.clear()
        }

        fun log(message: String) {
            if (INSTANCE == null) {
                LOG_BUFFER.add(message)
                return
            }

            try {
                INSTANCE!!.file.append(message).append('\n').flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}