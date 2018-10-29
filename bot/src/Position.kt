class Position(val x: Int, val y: Int) {

    internal fun directionalOffset(d: Direction): Position {
        val dx: Int
        val dy: Int

        when (d) {
            Direction.NORTH -> {
                dx = 0
                dy = -1
            }
            Direction.SOUTH -> {
                dx = 0
                dy = 1
            }
            Direction.EAST -> {
                dx = 1
                dy = 0
            }
            Direction.WEST -> {
                dx = -1
                dy = 0
            }
            Direction.STILL -> {
                dx = 0
                dy = 0
            }
        }

        return Position(x + dx, y + dy)
    }

    fun normalize(): Position {
        val x = (x % Game.map.size + Game.map.size) % Game.map.size
        val y = (y % Game.map.size + Game.map.size) % Game.map.size
        return Position(x, y)
    }

    override fun toString(): String {
        return "($x, $y)"
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is Position && other.x == x && other.y == y
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }
}