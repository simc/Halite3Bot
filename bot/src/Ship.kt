class Ship(owner: Int, id: Int, position: Position, val halite: Int) : Entity(owner, id, position) {

    enum class Task {
        NONE, DIG, GOTO_DROPOFF, BUILD_DROPOFF, END_GAME_SUICIDE
    }

    enum class NavAction {
        NONE, DIG, CHARGE, WAIT, MOVE, BUILD_DROPOFF
    }

    var target: MapCell? = null
    var task = Task.NONE
    var navAction = NavAction.NONE
    var navDirection: Direction? = null
    var commandSent = false

    val isFull: Boolean
        get() = halite >= Constants.MAX_HALITE * 0.95

    val canMove: Boolean
        get() = halite >= mapCell.leaveCost

    val isOnWayBack: Boolean
        get() = task == Task.GOTO_DROPOFF

    val isEndGameSuicide: Boolean
        get() = task == Task.END_GAME_SUICIDE

    val isNavigationFinished: Boolean
        get() = navAction != NavAction.NONE

    fun navMove(direction: Direction) {
        navAction = NavAction.MOVE
        navDirection = direction
    }

    fun navDig() {
        navAction = NavAction.DIG
        navDirection = Direction.STILL
    }

    fun navCharge() {
        navAction = NavAction.CHARGE
        navDirection = Direction.STILL
    }

    fun navWait() {
        navAction = NavAction.WAIT
        navDirection = Direction.STILL
    }

    fun targetDig() {
        navDig()
        targetPosition(position)
    }

    fun targetDropoff() {
        targetPosition(nextDropoff(position).position)
        Log.log("#$id targets dropoff")
    }

    fun targetPosition(position: Position) {
        removeTarget()
        val cell = Game.map.at(position)
        target = cell
        cell.targetOf.add(this)
    }

    private fun removeTarget() {
        if (target != null) {
            target!!.targetOf.remove(this)
            target = null
        }
    }

    fun update(oldShip: Ship) {
        task = oldShip.task
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