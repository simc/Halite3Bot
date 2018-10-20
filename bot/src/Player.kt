class Player private constructor(val id: Int, val shipyard: Shipyard) {
    var halite: Int = 0
    var ships = arrayListOf<Ship>()
    val dropoffs = arrayListOf<DropOff>()

    val allDropoffs: List<Entity>
        get() = dropoffs + shipyard

    internal fun _update(numShips: Int, numDropoffs: Int, halite: Int) {
        this.halite = halite

        val oldShips = ships
        ships = arrayListOf()
        for (i in 0 until numShips) {
            val ship = Ship._generate(id)
            val oldShip = oldShips.firstOrNull { it.id == ship.id }
            if (oldShip != null) {
                ship.update(oldShip)
            }
            ships.add(ship)
        }

        dropoffs.clear()
        for (i in 0 until numDropoffs) {
            dropoffs.add(DropOff.generate(id))
        }
    }

    companion object {

        internal fun _generate(): Player {
            val input = Input.readInput()

            val playerId = input.nextInt
            val shipyard_x = input.nextInt
            val shipyard_y = input.nextInt

            return Player(playerId, Shipyard(playerId, Position(shipyard_x, shipyard_y)))
        }
    }
}
