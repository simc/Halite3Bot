import kotlin.collections.ArrayList

class GameMap(val size: Int) {
    val cells: Array<Array<MapCell>>
    lateinit var flatCells: ArrayList<MapCell>
    var totalHalite = 0
    val currentHalite: Int
        get() {
            return (0 until size).sumBy { x ->
                (0 until size).sumBy { y ->
                    cells[x][y].halite
                }
            }
        }

    init {
        cells = (0 until size).map { x ->
            (0 until size).map { y ->
                MapCell(Position(x, y), 0)
            }.toTypedArray()
        }.toTypedArray()
    }

    fun at(position: Position): MapCell {
        val normalized = position.normalize()
        return cells[normalized.x][normalized.y]
    }

    fun at(entity: Entity): MapCell {
        return at(entity.position)
    }

    fun getUnsafeMoves(source: Position, destination: Position): ArrayList<Direction> {
        val possibleMoves = ArrayList<Direction>()

        val normalizedSource = source.normalize()
        val normalizedDestination = destination.normalize()

        val dx = Math.abs(normalizedSource.x - normalizedDestination.x)
        val dy = Math.abs(normalizedSource.y - normalizedDestination.y)
        val wrappedDx = size - dx
        val wrappedDy = size - dy

        if (normalizedSource.x < normalizedDestination.x) {
            possibleMoves.add(if (dx > wrappedDx) Direction.WEST else Direction.EAST)
        } else if (normalizedSource.x > normalizedDestination.x) {
            possibleMoves.add(if (dx < wrappedDx) Direction.WEST else Direction.EAST)
        }

        if (normalizedSource.y < normalizedDestination.y) {
            possibleMoves.add(if (dy > wrappedDy) Direction.NORTH else Direction.SOUTH)
        } else if (normalizedSource.y > normalizedDestination.y) {
            possibleMoves.add(if (dy < wrappedDy) Direction.NORTH else Direction.SOUTH)
        }

        return possibleMoves
    }

    fun update() {
        flatCells = arrayListOf()
        for (x in 0 until size) {
            for (y in 0 until size) {
                val cell = MapCell(Position(x, y), cells[x][y].halite)
                cells[x][y] = cell
                flatCells.add(cell)
            }
        }

        val updateCount = Input.readInput().nextInt

        for (i in 0 until updateCount) {
            val input = Input.readInput()
            val x = input.nextInt
            val y = input.nextInt

            cells[x][y].halite = input.nextInt
        }
    }

    companion object {
        internal fun _generate(): GameMap {
            val mapInput = Input.readInput()
            val size = mapInput.nextInt
            mapInput.nextInt // height ignored

            val map = GameMap(size)

            for (y in 0 until size) {
                val rowInput = Input.readInput()
                for (x in 0 until size) {
                    val halite = rowInput.nextInt
                    map.cells[x][y].halite = halite
                    map.totalHalite += halite
                }
            }

            return map
        }
    }
}