class Player constructor(val id: Int, val shipyard: Shipyard) {
    var halite: Int = 0
    var ships = arrayListOf<Ship>()
    val dropoffs = arrayListOf<Dropoff>()

    val allDropoffs: List<Entity>
        get() = dropoffs + shipyard

    fun update(numShips: Int, numDropoffs: Int, halite: Int) {
        this.halite = halite

        val oldShips = ships
        ships = arrayListOf()
        for (i in 0 until numShips) {
            ships.add(generateShip(oldShips))
        }

        dropoffs.clear()
        for (i in 0 until numDropoffs) {
            dropoffs.add(generateDropoff())
        }
    }

    private fun generateShip(oldShips: List<Ship>): Ship {
        val input = Input.readInput()

        val shipId = input.nextInt
        val x = input.nextInt
        val y = input.nextInt
        val halite = input.nextInt

        val oldShip = oldShips.firstOrNull { it.id == shipId }
        return Ship(id, shipId, Position(x, y), halite, oldShip)
    }

    private fun generateDropoff(): Dropoff {
        val input = Input.readInput()

        val dropoffId = input.nextInt
        val x = input.nextInt
        val y = input.nextInt

        return Dropoff(id, dropoffId, Position(x, y))
    }
}
