class Ship(owner: Int, id: Int, position: Position, val halite: Int, val oldShip: Ship?) : Entity(owner, id, position) {

    enum class Task {
        NONE, //No task
        DIG,
        DIG_FIELDS, //Dig normal fields from outside in
        DIG_REWARD, //Dig cells with higher than average reward
        DIG_SPECIAL_REWARD, //Dig cells with very, very high reward
        GOTO_DROPOFF, //Target next droppoff preferably choose a safe way
        BUILD_DROPOFF, //Go to a specific location and build a dropoff
        END_GAME_SUICIDE, //Go to next dropoff and kill yourself
        END_GAME_KAMIKAZE //Try to kill a high value enemy ship
    }

    enum class NavAction {
        NONE, DIG, CHARGE, WAIT, BLOCKED, MOVE, BUILD_DROPOFF
    }

    var target: Position? = null
        set(value) {
            if (field != null) {
                Game.map.at(field!!).targetOf.remove(this)
            }
            field = value
            if (value != null) {
                Game.map.at(value).targetOf.add(this)
            }
        }

    val reachedTarget: Boolean
        get() = target == null || target == position

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

    val isBuildingDropoff: Boolean
        get() = task == Task.BUILD_DROPOFF

    val isEndGameSuicide: Boolean
        get() = task == Task.END_GAME_SUICIDE

    val isEndGameKamikaze: Boolean
        get() = task == Task.END_GAME_KAMIKAZE

    val hasNavigation: Boolean
        get() = navAction != NavAction.NONE

    val dropoffCost: Int
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

    fun navBuildDropoff() {
        navAction = NavAction.BUILD_DROPOFF
        navDirection = Direction.STILL
    }

    fun isAllowedOnPosition(position: Position): Boolean {
        val cell = Game.map.at(position)
        val isDropoff = cell.structure?.isMine == true
        val noFullShipNearby = iterateByDistance(position, 2).none { it.ship?.isOnWayBack == true }
        return (!isDropoff || isOnWayBack || isEndGameSuicide || noFullShipNearby) && !cell.reserved
    }

    val optimalMoves: ArrayList<Direction>
        get() {
            val possibleMoves = ArrayList<Direction>()

            val dx = Math.abs(position.x - target!!.x)
            val dy = Math.abs(position.y - target!!.y)
            val wrappedDx = Game.map.size - dx
            val wrappedDy = Game.map.size - dy

            if (position.x < target!!.x) {
                possibleMoves.add(if (dx > wrappedDx) Direction.WEST else Direction.EAST)
            } else if (position.x > target!!.x) {
                possibleMoves.add(if (dx < wrappedDx) Direction.WEST else Direction.EAST)
            }

            if (position.y < target!!.y) {
                possibleMoves.add(if (dy > wrappedDy) Direction.NORTH else Direction.SOUTH)
            } else if (position.y > target!!.y) {
                possibleMoves.add(if (dy < wrappedDy) Direction.NORTH else Direction.SOUTH)
            }

            return possibleMoves
        }

    fun isGoodMove(direction: Direction): Boolean {
        return optimalMoves.contains(direction) && isAllowedOnPosition(position.directionalOffset(direction))
    }

    fun updateFromOldShip() {
        if (oldShip != null) {
            task = oldShip.task
            target = oldShip.target
        }
    }

    override fun toString(): String {
        return "Ship #$id (initPosition: $initialPosition, newPosition: $position, halite: $halite, task: $task, navAction: $navAction, target: ${target ?: "not set"}"
    }
}