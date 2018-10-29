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

    val ships: List<Ship>
        get() = players.flatMap { it.ships }

    val structures: List<Entity>
        get() = players.flatMap { it.allDropoffs }

    fun init() {
        Constants.populateConstants(Input.readLine())

        val input = Input.readInput()
        val numPlayers = input.nextInt
        myId = input.nextInt

        Log.open(myId)

        for (i in 0 until numPlayers) {
            players.add(generatePlayer())
        }
        map = GameMap._generate()
    }

    private fun generatePlayer(): Player {
        val input = Input.readInput()

        val playerId = input.nextInt
        val position = Position(input.nextInt, input.nextInt)
        return Player(playerId, Shipyard(playerId, position))
    }

    fun ready(name: String) {
        System.out.println(name)
    }

    fun updateFrame() {
        commands = arrayListOf()
        turnNumber = Input.readInput().nextInt

        Log.log("=============== TURN $turnNumber ================")

        history.turns.add(HistoryEntry(turnNumber))

        for (player in players) {
            val input = Input.readInput()

            @Suppress("UNUSED_VARIABLE")
            val playerId = input.nextInt
            val numShips = input.nextInt
            val numDropoffs = input.nextInt
            val halite = input.nextInt

            player.update(numShips, numDropoffs, halite)
        }

        map.update()

        //Last: add shipyards, ships and dropoffs to map
        ships.forEach {
            it.updatePosition()
            it.updateFromOldShip()
        }
        structures.forEach {
            it.updatePosition()
        }
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
        get() = turns[Game.turnNumber - 1]

    val lastTurn: HistoryEntry?
        get() = turns.getOrNull(Game.turnNumber - 2)
}

class HistoryEntry(val turnNumber: Int) {
    private val moves = arrayListOf<Pair<Int, Direction>>()

    var builtShip = false
    var plannedDropoff: Position? = null
    var reservedHalite = 0
    var builtDropoff = false

    fun doMove(ship: Ship, direction: Direction) {
        moves.add(Pair(ship.id, direction))
    }

    fun getMove(ship: Ship): Direction? {
        return moves.firstOrNull { it.first == ship.id }?.second
    }
}