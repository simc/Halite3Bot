class Ship(owner: Int, id: Int, position: Position, val halite: Int) : Entity(owner, id, position) {

    enum class Task {
        NONE, DIG, GOTO_DROP_OFF, BUILD_DROP_OFF, END_GAME_SUICIDE, END_GAME_KAMIKAZE
    }

    enum class NavAction {
        NONE, DIG, CHARGE, WAIT, MOVE, BUILD_DROP_OFF
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
        get() = task == Task.GOTO_DROP_OFF

    val isBuildingDropOff: Boolean
        get() = task == Task.BUILD_DROP_OFF

    val isEndGameSuicide: Boolean
        get() = task == Task.END_GAME_SUICIDE

    val isEndGameKamikaze: Boolean
        get() = task == Task.END_GAME_KAMIKAZE

    val isNavigationFinished: Boolean
        get() = navAction != NavAction.NONE

    val dropOffCost: Int
        get() = Constants.DROPOFF_COST - halite - mapCell.halite

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

    fun navBuildDropOff() {
        navAction = NavAction.BUILD_DROP_OFF
        navDirection = Direction.STILL
    }

    fun targetDropOff(): Boolean {
        return targetPosition(mapCell.nextDropOff.position)
    }

    fun targetPosition(targetPosition: Position): Boolean {
        removeTarget()
        val cell = Game.map.at(targetPosition)
        target = cell
        cell.targetOf.add(this)
        return position == targetPosition
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

    override fun toString(): String {
        return "Ship #$id (position: $initialPosition, newPosition: $position, halite: $halite, task: $task, navAction: $navAction, target: ${target?.position ?: "not set"})"
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