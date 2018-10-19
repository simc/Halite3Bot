import java.util.*

object Game {
    var turnNumber = 0
    var myId = -1
    val players: ArrayList<Player> = ArrayList()
    lateinit var me: Player
    lateinit var map: GameMap
    private var commands = arrayListOf<Command>()

    val turnsLeft: Int
        get() = Constants.MAX_TURNS - turnNumber - 1

    fun init() {
        Constants.populateConstants(Input.readLine())

        val input = Input.readInput()
        val numPlayers = input.nextInt
        myId = input.nextInt

        Log.open(myId)

        for (i in 0 until numPlayers) {
            players.add(Player._generate())
        }
        me = players.get(myId)
        map = GameMap._generate()
    }

    fun ready(name: String) {
        System.out.println(name)
    }

    fun updateFrame() {
        commands = arrayListOf()
        turnNumber = Input.readInput().nextInt

        Log.log("=============== TURN $turnNumber ================")

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
