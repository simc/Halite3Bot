
fun calculateDistance(source: Position, target: Position): Int {
    val normalizedSource = source.normalize()
    val normalizedTarget = target.normalize()

    val dx = Math.abs(normalizedSource.x - normalizedTarget.x)
    val dy = Math.abs(normalizedSource.y - normalizedTarget.y)

    val toroidalDx = Math.min(dx, Game.map.size - dx)
    val toroidalDy = Math.min(dy, Game.map.size - dy)

    return toroidalDx + toroidalDy
}

fun enemiesInRange(position: Position, range: Int): Int {
    return iterateByDistance(position, endDistance = range).sumBy {
        if (it.hasShip && it.ship!!.owner != Game.myId)
            1
        else
            0
    }
}

fun iterateByDistance(start: Position, startDistance: Int = 0, endDistance: Int? = null) = sequence {
    var tempStartDistance = startDistance
    val tempEndDistance = endDistance ?: Game.map.size - 1

    if (startDistance == 0) {
        yield(Game.map.at(start))
        //println("${start.x}, ${start.y}")
        tempStartDistance = 1
    }

    if (tempEndDistance == 0) return@sequence

    for (distance in tempStartDistance..tempEndDistance) {
        for (i in 0..(distance - 1)) {
            yield(Game.map.at(Position(start.x + i, start.y + distance - i)))
            //println("${start.x + i}, ${start.y + distance - i}")
            yield(Game.map.at(Position(start.x - i, start.y - distance + i)))
            //println("${start.x - i}, ${start.y - distance + i}")
            yield(Game.map.at(Position(start.x + distance - i, start.y - i)))
            //println("${start.x + distance - i}, ${start.y - i}")
            yield(Game.map.at(Position(start.x - distance + i, start.y + i)))
            //println("${start.x - distance + i}, ${start.y + i}")
        }
    }
}