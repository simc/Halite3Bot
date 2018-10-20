open class Entity(val owner: Int, val id: Int, var position: Position) {
    val mapCell: MapCell
        get() = Game.map.at(this)

    val isMine: Boolean
        get() = owner == Game.myId
}

class Shipyard(owner: Int, position: Position) : Entity(owner, 0, position) {

    fun spawn(): Command {
        return Command.spawnShip()
    }
}

class DropOff(owner: Int, id: Int, position: Position) : Entity(owner, id, position) {
    companion object {
        internal fun generate(playerId: Int): DropOff {
            val input = Input.readInput()

            val dropOffId = input.nextInt
            val x = input.nextInt
            val y = input.nextInt

            return DropOff(playerId, dropOffId, Position(x, y))
        }
    }
}