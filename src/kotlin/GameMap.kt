import java.util.*

class GameMap(val size: Int) {
    val cells: Array<Array<MapCell>>
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
        val normalized = normalize(position)
        return cells[normalized.x][normalized.y]
    }

    fun at(entity: Entity): MapCell {
        return at(entity.position)
    }

    fun getUnsafeMoves(source: Position, destination: Position): ArrayList<Direction> {
        val possibleMoves = ArrayList<Direction>()

        val normalizedSource = normalize(source)
        val normalizedDestination = normalize(destination)

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

    fun buildShip() {
        val ship = Ship(Game.myId, -1, Game.me.shipyard.position, 0)
        ship.mapCell.ship = ship
        ship.navigationFinished = true
        Game.me.ships.add(ship)
    }

    internal fun _update() {
        for (x in 0 until size) {
            for (y in 0 until size) {
                cells[x][y] = MapCell(Position(x, y), cells[x][y].halite)
            }
        }

        val updateCount = Input.readInput().nextInt

        for (i in 0 until updateCount) {
            val input = Input.readInput()
            val x = input.nextInt
            val y = input.nextInt

            cells[x][y].halite = input.nextInt
            if (Game.turnNumber == 1)
                totalHalite += cells[x][y].halite
        }

        //Update ships and structures in cells
        for (player in Game.players) {
            for (ship in player.ships) {
                ship.mapCell.ship = ship
            }

            player.shipyard.mapCell.structure = player.shipyard

            for (dropOff in player.dropoffs) {
                dropOff.mapCell.structure = dropOff
            }
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
                    map.cells[x][y].halite = rowInput.nextInt
                }
            }

            return map
        }
    }
}