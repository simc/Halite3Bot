import Constants.INSPIRATION_RADIUS
import Constants.INSPIRATION_SHIP_COUNT
import Constants.INSPIRED_BONUS_MULTIPLIER
import Constants.INSPIRED_EXTRACT_RATIO

class MapCell(val position: Position, var halite: Int) {
    var ship: Ship? = null
    var structure: Entity? = null
    var targetOf = arrayListOf<Ship>()
    var passCount = 0
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

    var reward: Int = -1
        get() {
            if (field == -1) {
                if (enemiesInRange(position, INSPIRATION_RADIUS) >= INSPIRATION_SHIP_COUNT) {
                    val extractAmount = halite.toDouble() / INSPIRED_EXTRACT_RATIO
                    val extractBonus = extractAmount / INSPIRED_BONUS_MULTIPLIER
                    field = Math.ceil(extractAmount + extractBonus).toInt()
                } else {
                    field = Math.ceil(halite.toDouble() / INSPIRED_EXTRACT_RATIO).toInt()
                }
            }
            return field
        }
        private set

    val reachableCells: List<MapCell>
        get() = listOf(
                Game.map.at(position.directionalOffset(Direction.NORTH)),
                Game.map.at(position.directionalOffset(Direction.SOUTH)),
                Game.map.at(position.directionalOffset(Direction.EAST)),
                Game.map.at(position.directionalOffset(Direction.WEST))
        )


    override fun equals(other: Any?): Boolean {
        return other != null && other is MapCell && other.position == position
    }

    override fun hashCode(): Int {
        return position.hashCode()
    }
}
