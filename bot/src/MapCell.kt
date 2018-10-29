import Constants.EXTRACT_RATIO
import Constants.INSPIRATION_RADIUS
import Constants.INSPIRATION_SHIP_COUNT
import Constants.INSPIRED_BONUS_MULTIPLIER
import Constants.INSPIRED_EXTRACT_RATIO

class MapCell(val position: Position, var halite: Int) {
    var ship: Ship? = null
        @Deprecated("Updated by entity") set
    var structure: Entity? = null
        @Deprecated("Updated by entity") set

    var targetOf = arrayListOf<Ship>()
    var reserved = false // No ship is allowed to move to this cell

    val isEmpty: Boolean
        get() = ship == null && structure == null

    val hasShip: Boolean
        get() = ship != null

    val hasStructure: Boolean
        get() = structure != null

    val isTargetEmpty: Boolean
        get() = !hasShip && targetOf.isEmpty()

    val leaveCost: Int
        get() = halite / Constants.MOVE_COST_RATIO

    val miningReward: Int
        get() = Math.ceil(halite.toDouble() / EXTRACT_RATIO).toInt()

    private var inspirationRewardCache: Int? = null
    val inspirationReward: Int
        get() {
            if (inspirationRewardCache == null) {
                inspirationRewardCache = if (enemiesInRange(position, INSPIRATION_RADIUS) >= INSPIRATION_SHIP_COUNT)
                    Math.ceil(miningReward  * INSPIRED_BONUS_MULTIPLIER).toInt()
                else
                    0
            }

            return inspirationRewardCache!!
        }

    val reward: Int
        get() = miningReward + inspirationReward

    val reachableCells: List<MapCell>
        get() = listOf(
                Game.map.at(position.directionalOffset(Direction.NORTH)),
                Game.map.at(position.directionalOffset(Direction.SOUTH)),
                Game.map.at(position.directionalOffset(Direction.EAST)),
                Game.map.at(position.directionalOffset(Direction.WEST))
        )

    private var nextDropoffCache: Entity? = null
    val nextDropoff: Entity
        get() {
            if (nextDropoffCache == null) {
                var minDistance = Int.MAX_VALUE
                for (dropoff in Game.me.allDropoffs) {
                    val distance = calculateDistance(dropoff.position, position)
                    if (distance < minDistance) {
                        nextDropoffCache = dropoff
                        minDistance = distance
                    }
                }
            }

            return nextDropoffCache!!
        }

    val nextDropoffDistance: Int
        get() = calculateDistance(nextDropoff.position, position)

    val fieldEdges = booleanArrayOf(false, false, false, false)

    override fun toString(): String {
        return "cell $position " + targetOf.joinToString()
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is MapCell && other.position == position
    }

    override fun hashCode(): Int {
        return position.hashCode()
    }
}
