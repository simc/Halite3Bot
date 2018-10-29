fun drawMap(cellContent: (position: Position) -> String) {
    val title = (0 until Game.map.size).joinToString(separator = "|", transform = {if (it < 10) " $it" else "$it"})
    Log.log("   $title")

    for (y in 0 until Game.map.size) {
        val row = (0 until Game.map.size).joinToString(separator = "") { x ->
            cellContent(Position(x, y))
        }
        Log.log("${if (y < 10) " $y" else "$y"} [$row]")
    }
}

fun drawHaliteFields(fields: List<List<MapCell>>, drawBorders: Boolean, possibleDropoffs: List<Position> = arrayListOf()) {
    val fieldCells = fields.flatten()
    drawMap { position ->
        if (Game.me.shipyard.position == position) {
            " \uD83C\uDFE1 "
        } else if (Game.me.dropoffs.any { it.position == position }) {
            " \uD83C\uDFE0 "
        } else if (possibleDropoffs.contains(position)) {
            " ❌ "
        } else {
            val fieldCell = fieldCells.firstOrNull { it.position == position }
            if (fieldCell != null) {
                val borderCount = fieldCell.fieldEdges.count { it }
                if (drawBorders && borderCount > 0) {
                    if (borderCount == 2)
                        " O "
                    else if (borderCount == 4)
                        " ${fieldCell.halite / 100} "
                    else if (fieldCell.fieldEdges[0])
                        "‾‾‾"
                    else if (fieldCell.fieldEdges[1])
                        "___"
                    else
                        " | "
                } else {
                    " ${fieldCell.halite / 100} "
                }
            } else {
                "   "
            }
        }
    }
}

fun drawShipTargets() {
    fun formatShipId(ship: Ship): String {
        var shipId = ship.id.toString()
        if (shipId.length == 1)
            shipId = "0$shipId"
        return shipId
    }

    drawMap { position ->
        val cell = Game.map.at(position)
        if (cell.hasShip && cell.ship!!.isMine) {
            val ship = cell.ship!!
            val shipId = formatShipId(cell.ship!!)
            /*when (ship.navAction) {
                Ship.NavAction.NONE -> "?${formatShipId(cell.ship!!)}"
                Ship.NavAction.DIG -> "⛏️$shipId"
                Ship.NavAction.CHARGE -> "\uD83D\uDD0C️$shipId"
                Ship.NavAction.WAIT -> "⏳$shipId"
                Ship.NavAction.BLOCKED -> "\uD83D\uDEA7️$shipId"
                Ship.NavAction.MOVE -> "⛵️$shipId"
                Ship.NavAction.BUILD_DROPOFF -> "D️$shipId"
            }*/
            when (ship.navAction) {
                Ship.NavAction.NONE -> "?${formatShipId(cell.ship!!)}"
                Ship.NavAction.DIG -> "D$shipId"
                Ship.NavAction.CHARGE -> "C$shipId"
                Ship.NavAction.WAIT -> "W$shipId"
                Ship.NavAction.BLOCKED -> "B$shipId"
                Ship.NavAction.MOVE -> "S$shipId"
                Ship.NavAction.BUILD_DROPOFF -> "B$shipId"
            }

        } else if (cell.targetOf.size > 1) {
            " T "
        } else if (cell.targetOf.size == 1) {
            "T${formatShipId(cell.targetOf.first())}"
        } else if (cell.position == Game.me.shipyard.position) {
            " SY"
        } else {
            "   "
        }
        /*
        } else if (cell.targetOf.size > 1) {
            " \uD83D\uDEA9 "
        } else if (cell.targetOf.size == 1) {
            "\uD83D\uDEA9${formatShipId(cell.targetOf.first())}"
        } else if (cell.position == Game.me.shipyard.position) {
            " \uD83C\uDFE0 "
        } else {
            "   "
        }
         */
    }
}