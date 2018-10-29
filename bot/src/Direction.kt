enum class Direction constructor(val charValue: Char) {
    NORTH('n'),
    EAST('e'),
    SOUTH('s'),
    WEST('w'),
    STILL('o');

    fun invertDirection(): Direction {
        return when (this) {
            NORTH -> SOUTH
            EAST -> WEST
            SOUTH -> NORTH
            WEST -> EAST
            STILL -> STILL
        }
    }

    companion object {
        val ALL_CARDINALS = arrayListOf(NORTH, SOUTH, EAST, WEST)
    }
}
