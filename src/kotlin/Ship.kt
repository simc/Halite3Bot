class Ship(owner: Int, id: Int, position: Position, val halite: Int) : Entity(owner, id, position) {

    var target: MapCell? = null
    var onWayBack = false
    var navigationFinished = false
    var endGameSuicide = false

    val isFull: Boolean
        get() = halite >= Constants.MAX_HALITE * 0.95

    fun removeTarget() {
        if (target != null) {
            target!!.targetOf.remove(this)
            target = null
        }
    }

    fun targetDropoff() {
        onWayBack = true
        removeTarget()
        target = nextDropoff(position).mapCell
        target!!.targetOf.add(this)
        Log.log("#$id targets dropoff")
    }

    fun dig() {
        removeTarget()
        target = mapCell
        mapCell.targetOf.add(this)
    }

    fun targetPosition(position: Position) {
        removeTarget()
        val cell = Game.map.at(position)
        target = cell
        cell.targetOf.add(this)
    }

    val canMove: Boolean
        get() = halite >= mapCell.leaveCost

    fun update(oldShip: Ship) {
        onWayBack = oldShip.onWayBack
    }

    companion object {
        internal fun _generate(playerId: Int): Ship {
            val input = Input.readInput()

            val shipId = input.nextInt
            val x = input.nextInt
            val y = input.nextInt
            val halite = input.nextInt

            return Ship(playerId, shipId, Position(x, y), halite)
        }
    }
}