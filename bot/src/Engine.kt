import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

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

object Constants {
    /** The maximum amount of halite a ship can carry.  */
    var MAX_HALITE: Int = 0
    /** The cost to build a single ship.  */
    var SHIP_COST: Int = 0
    /** The cost to build a dropoff.  */
    var DROPOFF_COST: Int = 0
    /** The maximum number of turns a game can last.  */
    var MAX_TURNS: Int = 0
    /** 1/EXTRACT_RATIO halite (rounded) is collected from a square per turn.  */
    var EXTRACT_RATIO: Int = 0
    /** 1/MOVE_COST_RATIO halite (rounded) is needed to move off a cell.  */
    var MOVE_COST_RATIO: Int = 0
    /** Whether inspiration is enabled.  */
    var INSPIRATION_ENABLED: Boolean = false
    /** A ship is inspired if at least INSPIRATION_SHIP_COUNT opponent ships are within this Manhattan distance.  */
    var INSPIRATION_RADIUS: Int = 0
    /** A ship is inspired if at least this many opponent ships are within INSPIRATION_RADIUS distance.  */
    var INSPIRATION_SHIP_COUNT: Int = 0
    /** An inspired ship mines 1/X halite from a cell per turn instead.  */
    var INSPIRED_EXTRACT_RATIO: Int = 0
    /** An inspired ship that removes Y halite from a cell collects X*Y additional halite.  */
    var INSPIRED_BONUS_MULTIPLIER: Double = 0.toDouble()
    /** An inspired ship instead spends 1/X% halite to move.  */
    var INSPIRED_MOVE_COST_RATIO: Int = 0

    fun populateConstants(stringFromEngine: String?) {
        val rawTokens = stringFromEngine?.split("[{}, :\"]+".toRegex()) ?: ArrayList()
        val tokens = ArrayList<String>()
        for (i in rawTokens.indices) {
            if (!rawTokens[i].isEmpty()) {
                tokens.add(rawTokens[i])
            }
        }

        Log.log(tokens.toString())
        if (tokens.size % 2 != 0) {
            Log.log("Error: constants: expected even total number of key and value tokens from server.")
            throw IllegalStateException()
        }

        val constantsMap = HashMap<String, String>()

        var i = 0
        while (i < tokens.size) {
            constantsMap[tokens[i]] = tokens[i + 1]
            i += 2
        }

        SHIP_COST = getInt(constantsMap, "NEW_ENTITY_ENERGY_COST")
        DROPOFF_COST = getInt(constantsMap, "DROPOFF_COST")
        MAX_HALITE = getInt(constantsMap, "MAX_ENERGY")
        MAX_TURNS = getInt(constantsMap, "MAX_TURNS")
        EXTRACT_RATIO = getInt(constantsMap, "EXTRACT_RATIO")
        MOVE_COST_RATIO = getInt(constantsMap, "MOVE_COST_RATIO")
        INSPIRATION_ENABLED = getBoolean(constantsMap, "INSPIRATION_ENABLED")
        INSPIRATION_RADIUS = getInt(constantsMap, "INSPIRATION_RADIUS")
        INSPIRATION_SHIP_COUNT = getInt(constantsMap, "INSPIRATION_SHIP_COUNT")
        INSPIRED_EXTRACT_RATIO = getInt(constantsMap, "INSPIRED_EXTRACT_RATIO")
        INSPIRED_BONUS_MULTIPLIER = getDouble(constantsMap, "INSPIRED_BONUS_MULTIPLIER") ?: 0.0
        INSPIRED_MOVE_COST_RATIO = getInt(constantsMap, "INSPIRED_MOVE_COST_RATIO")
    }

    private fun getInt(map: Map<String, String>, key: String): Int {
        return Integer.parseInt(getString(map, key))
    }

    private fun getDouble(map: Map<String, String>, key: String): Double? {
        return getString(map, key)?.toDouble()
    }

    private fun getBoolean(map: Map<String, String>, key: String): Boolean {
        val stringValue = getString(map, key)
        return when (stringValue) {
            "true" -> true
            "false" -> false
            else -> {
                Log.log("Error: constants: " + key + " constant has value of '" + stringValue +
                        "' from server. Do not know how to parse that as boolean.")
                throw IllegalStateException()
            }
        }
    }

    private fun getString(map: Map<String, String>, key: String): String? {
        if (!map.containsKey(key)) {
            Log.log("Error: constants: server did not send $key constant.")
            throw IllegalStateException()
        }
        return map[key]
    }
}

enum class Direction constructor(val charValue: Char) {
    NORTH('n'),
    EAST('e'),
    SOUTH('s'),
    WEST('w'),
    STILL('o');

    fun invertDirection(): Direction {
        when (this) {
            NORTH -> return SOUTH
            EAST -> return WEST
            SOUTH -> return NORTH
            WEST -> return EAST
            STILL -> return STILL
            else -> throw IllegalStateException("Unknown direction " + this)
        }
    }

    companion object {
        val ALL_CARDINALS = arrayListOf(NORTH, SOUTH, EAST, WEST)
    }
}

class Position(val x: Int, val y: Int) {

    internal fun directionalOffset(d: Direction): Position {
        val dx: Int
        val dy: Int

        when (d) {
            Direction.NORTH -> {
                dx = 0
                dy = -1
            }
            Direction.SOUTH -> {
                dx = 0
                dy = 1
            }
            Direction.EAST -> {
                dx = 1
                dy = 0
            }
            Direction.WEST -> {
                dx = -1
                dy = 0
            }
            Direction.STILL -> {
                dx = 0
                dy = 0
            }
        }

        return Position(x + dx, y + dy)
    }

    fun normalize(): Position {
        val x = (x % Game.map.size + Game.map.size) % Game.map.size
        val y = (y % Game.map.size + Game.map.size) % Game.map.size
        return Position(x, y)
    }

    override fun toString(): String {
        return "($x, $y)"
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is Position && other.x == x && other.y == y
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
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

            val now_in_nanos = System.nanoTime()
            val filename = "bot-unknown-$now_in_nanos.log"
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