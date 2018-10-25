open class Entity(val owner: Int, val id: Int, val initialPosition: Position) {
    var position = initialPosition
        @Suppress("DEPRECATION")
        set(value) {
            if (this is Ship) {
                if (mapCell.ship == this)
                    mapCell.ship = null
            } else {
                if (mapCell.structure == this)
                    mapCell.structure = null
            }

            field = value.normalize()
            if (this is Ship)
                mapCell.ship = this
            else
                mapCell.structure = this
        }

    fun updatePosition() {
        position = position
    }

    val mapCell: MapCell
        get() = Game.map.cells[position.x][position.y]

    val isMine: Boolean
        get() = owner == Game.myId
}

class Shipyard(owner: Int, position: Position) : Entity(owner, 0, position)

class Dropoff(owner: Int, id: Int, position: Position) : Entity(owner, id, position)