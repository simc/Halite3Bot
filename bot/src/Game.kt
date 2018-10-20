import java.util.*

object Game {
    var turnNumber = 0
    var myId = -1

    val players: ArrayList<Player> = ArrayList()
    lateinit var map: GameMap

    val history = History()

    private var commands = arrayListOf<Command>()

    val me: Player
        get() = players[myId]

    val turnsLeft: Int
        get() = Constants.MAX_TURNS - turnNumber

    fun init() {
        Constants.populateConstants(Input.readLine())

        val input = Input.readInput()
        val numPlayers = input.nextInt
        myId = input.nextInt

        Log.open(myId)

        for (i in 0 until numPlayers) {
            players.add(Player._generate())
        }
        map = GameMap._generate()
    }

    fun ready(name: String) {
        System.out.println(name)
    }

    fun updateFrame() {
        commands = arrayListOf()
        turnNumber = Input.readInput().nextInt - 1

        Log.log("=============== TURN $turnNumber ================")

        history.turns.add(HistoryEntry(turnNumber))

        for (i in 0 until players.size) {
            val input = Input.readInput()

            val currentPlayerId = input.nextInt
            val numShips = input.nextInt
            val numDropoffs = input.nextInt
            val halite = input.nextInt

            players.get(currentPlayerId)._update(numShips, numDropoffs, halite)
        }

        map._update()
    }

    fun sendCommand(command: Command) {
        commands.add(command)
    }

    fun endTurn() {
        for (command in commands) {
            System.out.print(command.command)
            System.out.print(' ')
        }
        System.out.println()
    }
}

class History {
    val turns = arrayListOf<HistoryEntry>()

    val currentTurn: HistoryEntry
        get() = turns[Game.turnNumber]

    val lastTurn: HistoryEntry?
        get() = turns.getOrNull(Game.turnNumber - 1)
}

class HistoryEntry(val turnNumber: Int) {
    private val moves = arrayListOf<Pair<Int, Direction>>()

    var builtShip = false
    var planBuildingDropOff = false
    var buildDropoff = false

    fun doMove(ship: Ship, direction: Direction) {
        moves.add(Pair(ship.id, direction))
    }

    fun getMove(ship: Ship): Direction? {
        return moves.firstOrNull { it.first == ship.id }?.second
    }
}