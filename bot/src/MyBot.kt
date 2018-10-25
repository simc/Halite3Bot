import Constants.SHIP_COST
import java.util.*

object MyBot {
    val optimalStops = hashMapOf(
            32 to Pair(191, 156),
            40 to Pair(226, 183),
            48 to Pair(253, 214),
            56 to Pair(278, 237),
            64 to Pair(307, 269)
    )

    private fun checkShouldBuildShip() {
        val (two, four) = optimalStops[Game.map.size]!!
        val optimalNumber = if (Game.players.size == 2) two else four
        if (Game.turnNumber > optimalNumber) {
            return
        }

        //Is there enough halite for building a ship (and maybe a dropoff)
        val halite = Game.me.halite - Game.history.currentTurn.reservedHalite
        val enoughHalite = halite >= SHIP_COST

        //Can a ship on the shipyard leave for sure
        val shipyardCell = Game.me.shipyard.mapCell
        val shipyardDirectionFree = !shipyardCell.hasShip || shipyardCell.reachableCells.any { it.isEmpty }

        //If there are ships waiting near the shipyard, there is a ship built only every third frame
        val shipsWaiting = shipyardCell.reachableCells.any { it.ship?.isOnWayBack == true }
        val lastTurnsShipBuilt = Game.history.turns.takeLast(2).any(HistoryEntry::builtShip)
        val disableBuildShip = shipsWaiting && lastTurnsShipBuilt

        if (enoughHalite && shipyardDirectionFree && !disableBuildShip) {
            Game.sendCommand(Command.spawnShip())
            Game.history.currentTurn.builtShip = true
            shipyardCell.reserved = true
            Game.me.halite -= SHIP_COST
            Log.log("Building ship")
        }
    }

    /*private fun findHaliteFields(): ArrayList<Pair<MapCell, Int>> {
        val possibleSpots = arrayListOf<Pair<MapCell, Int>>()
        for (x in 0 until Game.map.size) {
            for (y in 0 until Game.map.size) {
                val cell = Game.map.cells[x][y]
                if (cell.halite < 300) continue

                val spotHalite = cell.halite + iterateByDistance(cell.position, 0, 5).sumBy {
                    if (cell.halite < 300) 0 else it.halite
                }

                if (spotHalite < 20000) continue

                //Next drop off at least 15 away
                //if (cell.nextDropoffDistance < 15) continue

                val spotNearIndex = possibleSpots.indexOfFirst {
                    calculateDistance(it.first.position, cell.position) < 8
                }

                if (spotNearIndex == -1) {
                    possibleSpots.add(Pair(cell, spotHalite))
                } else {
                    val otherSpot = possibleSpots[spotNearIndex]
                    if (otherSpot.second < spotHalite) {
                        possibleSpots[spotNearIndex] = Pair(cell, spotHalite)
                    }
                }
            }
        }

        return possibleSpots
    }

    private fun checkShouldBuildDropoff() {
        findHaliteFields().sortedByDescending { it.second }.forEach {
            Log.log("Field: ${it.first}: ${it.second}")
        }
        /*if (Game.me.ships.size.toDouble() / Game.me.allDropoffs.size < 12) {
            Game.me.ships.forEach {
                if (it.isBuildingDropoff) {
                    it.task = Ship.Task.NONE
                    Log.log("cancel dropoff")
                }
            }
            return
        }

        val possibleDropoffs = findDropoffLocations().filter {
            it.first.nextDropoffDistance < 25
        }
        val newDropoff = possibleDropoffs.sortedBy {
            it.second
        }.firstOrNull()

        val plannedDropoff = Game.history.currentTurn.plannedDropoff
        if (plannedDropoff != null && possibleDropoffs.any { it.first.position == plannedDropoff }) {
            Game.history.currentTurn.reservedHalite = DROPOFF_COST - Game.map.at(plannedDropoff).halite
            findShipToBuildDropoff(plannedDropoff)

        } else if (newDropoff != null) {
            Game.history.currentTurn.reservedHalite = DROPOFF_COST - newDropoff.first.halite
            findShipToBuildDropoff(newDropoff.first.position)

        } else {
            Game.history.currentTurn.reservedHalite = 0
            Game.me.ships.forEach {
                if (it.isBuildingDropoff) {
                    it.task = Ship.Task.NONE
                    Log.log("cancel dropoff")
                }
            }
        }*/
    }

    private fun findShipToBuildDropoff(dropoff: Position) {
        Log.log("plan building dropoff ${Game.me.halite}")

        Game.history.currentTurn.plannedDropoff = dropoff
        iterateByDistance(dropoff).forEach {
            val ship = it.ship
            if (ship?.isMine == true) {
                buildDropoff(dropoff, ship)
                return
            }
        }
    }

    private fun buildDropoff(dropoff: Position, ship: Ship) {
        ship.task = Ship.Task.BUILD_DROPOFF
        if (ship.targetPosition(dropoff)) {
            if (Game.me.halite >= ship.dropoffCost) {
                Game.history.currentTurn.plannedDropoff = null
                ship.navBuildDropoff()
                Game.me.halite -= ship.dropoffCost
                Log.log("build dropoff")

            } else {
                ship.navWait()
                Log.log("wait dropoff")
            }

        } else {
            Log.log("move dropoff")
        }

        Game.me.ships.forEach {
            if (it != ship && it.isBuildingDropoff) {
                it.task = Ship.Task.NONE
                Log.log("change dropoff ship")
            }
        }
    }

    private fun defaultStrategy() {
        val idleShips = arrayListOf<Ship>()

        for (ship in Game.me.ships) {
            if (ship.isBuildingDropoff) {
                continue

            } else if ((ship.isOnWayBack && ship.halite > 500) || ship.isFull) {
                Log.log("ship #${ship.id} comes back")
                ship.task = Ship.Task.GOTO_DROPOFF
                ship.targetDropoff()

            } else if (ship.mapCell.reward > 20) {
                ship.task = Ship.Task.DIG
                ship.navDig()

            } else {
                if (ship.oldShip?.navAction == Ship.NavAction.DIG)
                    ship.task = Ship.Task.NONE

                idleShips.add(ship)
            }
        }

        val cells = Game.map.flatCells
        val remainingShips = idleShips.filter { ship ->
            val oldTarget = ship.oldShip?.target

            if (oldTarget != null && ship.task == Ship.Task.DIG && cells.contains(oldTarget)) {
                if (ship.targetPosition(ship.oldShip!!.target!!.position)) {
                    if (ship.oldShip!!.navAction != Ship.NavAction.DIG) {
                        ship.navDig()
                        return@filter false
                    }
                    //Digging is already finished

                } else {
                    return@filter false
                }
            }

            true
        }

        remainingShips.forEach {
            findBestTargetForShip(it)
        }

        Game.me.ships.forEach { ship ->
            if (!ship.canMove) {
                ship.navCharge()
            }
        }
    }

    private fun findBestTargetForShip(ship: Ship) {
        val cells = Game.map.flatCells
        ship.task = Ship.Task.DIG

        val target = cells.mapNotNull { cell ->
            if (cell.targetOf.isNotEmpty() || cell.reward < 20)
                return@mapNotNull null

            val nearbyShipsPenalty = - iterateByDistance(cell.position, 1, 2).sumBy {
                if (it.targetOf.isNotEmpty())
                    200 / calculateDistance(cell.position, it.position)
                else
                    0
            }
            val distance = calculateDistance(cell.position, ship.position)
            val distancePenalty = - distance * 15
            val dropoffDistancePenalty = - cell.nextDropoffDistance * 15
            val score = cell.halite + nearbyShipsPenalty + distancePenalty + dropoffDistancePenalty

            if (score < 0) {
                null
            } else {
                cell to score
            }

        }.maxBy {
            Log.log("${it.first}: ${it.second}")
            it.second

        }?.first

        if (target != null) {
            if (ship.targetPosition(target.position)) {
                Log.log("digging at target")
                ship.navDig()
            }
            return
        }

        for (cell in iterateByDistance(ship.position)) {
            if (cell.reward > 35 && cell.isTargetEmpty || cell.ship == ship) {
                if (ship.targetPosition(cell.position)) {
                    ship.navDig()
                    Log.log("SHIP ${ship.id} HAS RANDOM TASK")
                }
                return
            }
        }

        Log.log("SHIP HAS NO TASK")

        //Ship has no task
        //ship.navDig()
    }


    private fun endGameSuicideStrategy() {
        val ships = Game.me.ships

        if (ships.isEmpty())
            return

        val (importantShips, otherShips) = ships.partition {
            it.halite > 0
        }
        val shipToDropoffDistances = importantShips.map {
            it.mapCell.nextDropoffDistance
        }

        if (importantShips.isEmpty() || shipToDropoffDistances.max()!! >= Game.turnsLeft) {
            importantShips.forEach { ship ->
                ship.task = Ship.Task.END_GAME_SUICIDE

                if (!ship.canMove) {
                    ship.navCharge()
                } else {
                    if (ship.targetDropoff()) {
                        ship.navWait()
                    }
                }
            }

            otherShips.forEach {
                it.task = Ship.Task.END_GAME_KAMIKAZE
                if (it.targetPosition(Game.players[1].shipyard.position)) {
                    it.navWait()
                }
            }

        } else {
            defaultStrategy()
        }
    }*/

    private var haliteFields = arrayListOf<List<MapCell>>()

    private fun calculateHalieFields(): ArrayList<List<MapCell>> {
        val cells = Game.map.flatCells
        val average = Game.map.flatCells.sumBy { it.halite } / cells.size
        val threshold = (average * 1.5).toInt().coerceIn(100, 200)

        val visited = Array(Game.map.size) { BooleanArray(Game.map.size) }
        val fields = arrayListOf<List<MapCell>>()
        val stack = Stack<MapCell>()

        for (cell in cells) {
            if (visited[cell.position.x][cell.position.y])
                continue

            if (cell.halite >= threshold) {
                val field = arrayListOf(cell)
                stack.push(cell)
                while (stack.isNotEmpty()) {
                    val testCell = stack.pop()
                    visited[testCell.position.x][testCell.position.y] = true

                    testCell.reachableCells.forEachIndexed { index, neighbor ->
                        if (neighbor.halite < threshold) {
                            testCell.fieldEdges[index] = true
                        } else if (!visited[neighbor.position.x][neighbor.position.y]) {
                            field.add(neighbor)
                            stack.push(neighbor)
                        }
                    }
                }

                fields.add(field)
            }
        }

        return fields
    }

    fun defaultStrategy() {
        val fieldCells = haliteFields.flatten()

        val edges = fieldCells.filter { it.fieldEdges.count { it } > 0 }

        val idleShips = Game.me.ships.filter idle@{ ship ->
            if (ship.isBuildingDropoff) {
                return@idle false

            } else if ((ship.isOnWayBack && ship.halite > 500) || ship.isFull) {
                Log.log("ship #${ship.id} comes back")
                ship.task = Ship.Task.GOTO_DROPOFF
                ship.target = ship.mapCell.nextDropoff.position

                if (!ship.canMove)
                    ship.navCharge()

                return@idle false

            } else if (ship.task == Ship.Task.DIG) {
                if (ship.reachedTarget) {
                    if (ship.mapCell.reward > 20) {
                        ship.navDig()
                        return@idle false

                    } else {
                        ship.task = Ship.Task.NONE
                    }

                } else {
                    if (Game.map.at(ship.target!!).ship?.isMine == false) {
                        ship.task = Ship.Task.NONE

                    } else {
                        if (!ship.canMove)
                            ship.navCharge()
                        return@idle false
                    }
                }
            }

            true
        }

        idleShips.forEach { ship ->
            ship.task = Ship.Task.DIG
            val nearestEdge = edges.filter {
                it.targetOf.isEmpty()
            }.minBy { edge ->
                calculateDistance(ship.position, edge.position)// + edge.nextDropoffDistance
            }

            if (nearestEdge != null) {
                ship.target = nearestEdge.position

                if (!ship.canMove) {
                    ship.navWait()
                }
                Log.log("NEW TARGET: $ship")
                if (ship.reachedTarget) {
                    ship.navDig()
                }
            } else {
                ship.target = Position(0, 0)
                if (ship.reachedTarget)
                    ship.navWait()
            }
        }
    }

    private fun endGameSuicideStrategy() {
        val ships = Game.me.ships

        if (ships.isEmpty())
            return

        val (importantShips, otherShips) = ships.partition {
            it.halite > 0 || it.mapCell.nextDropoffDistance <= 3
        }
        val shipToDropoffDistances = importantShips.map {
            it.mapCell.nextDropoffDistance
        }

        if (importantShips.isEmpty() || shipToDropoffDistances.max()!! >= Game.turnsLeft) {
            importantShips.forEach { ship ->
                ship.task = Ship.Task.END_GAME_SUICIDE

                if (!ship.canMove) {
                    ship.navCharge()
                } else {
                    ship.target = ship.mapCell.nextDropoff.position
                    if (ship.reachedTarget) {
                        ship.navWait()
                    }
                }
            }

            otherShips.forEach { ship ->
                ship.task = Ship.Task.END_GAME_KAMIKAZE
                ship.target = Game.players[1].shipyard.position
                if (ship.reachedTarget) {
                    ship.navWait()
                }
            }

        } else {
            defaultStrategy()
        }
    }

    private fun checkShouldBuildDropoff() {
        val fieldCells = haliteFields.flatten()
        val goodTargetsNearDropoff = fieldCells.count { it.nextDropoffDistance < 6 }
        if (goodTargetsNearDropoff > Game.me.ships.size)
            return

        val possibleDropoffs = haliteFields.mapNotNull { field ->
            var haliteSum = 0
            var minDistance = Int.MAX_VALUE
            lateinit var minDistanceCell: MapCell
            field.forEach { cell ->
                haliteSum += cell.halite
                if (cell.nextDropoffDistance < minDistance) {
                    minDistance = cell.nextDropoffDistance
                    minDistanceCell = cell
                }
            }

            if ((15..25).contains(minDistance) && haliteSum > 10000)
                Pair(minDistanceCell, haliteSum)
            else
                null
        }

        /*val possibleDropoffs = fieldCells.mapNotNull {
            if ((15..25).contains(it.nextDropoffDistance)) {
                val haliteNear = iterateByDistance(it.position, 0, 5).sumBy { it.reward }
                if (haliteNear > 500)
                    Pair(it, haliteNear)
                else
                    null
            } else {
                null
            }
        }*/

        //val pos = possibleDropoffs.sortedByDescending { it.second }.take(5).map { it.first.position }
        //drawHaliteFields(haliteFields, false, pos)
    }

    private fun findDropoffLocations(): ArrayList<Pair<MapCell, Int>> {
        val possibleSpots = arrayListOf<Pair<MapCell, Int>>()
        for (x in 0 until Game.map.size) {
            for (y in 0 until Game.map.size) {
                val cell = Game.map.cells[x][y]
                if (cell.halite < 300) continue

                val spotHalite = cell.halite + iterateByDistance(cell.position, 0, 4).sumBy { it.halite }

                if (spotHalite < 15000) continue

                //Next drop off at least 15 away
                if (cell.nextDropoffDistance < 15) continue

                val spotNearIndex = possibleSpots.indexOfFirst {
                    calculateDistance(it.first.position, cell.position) < 6
                }

                if (spotNearIndex == -1) {
                    possibleSpots.add(Pair(cell, spotHalite))
                } else {
                    val otherSpot = possibleSpots[spotNearIndex]
                    if (otherSpot.second < spotHalite) {
                        possibleSpots[spotNearIndex] = Pair(cell, spotHalite)
                    }
                }
            }
        }

        return possibleSpots
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Game.init()
        Game.ready("leisim")

        Log.log("MY BOT! Successfully created bot! My Player ID is ${Game.myId}.")

        while (true) {
            val start = System.currentTimeMillis()
            Game.updateFrame()

            haliteFields = calculateHalieFields()

            checkShouldBuildDropoff()
            checkShouldBuildShip()

            /*checkShouldBuildDropoff()
            checkShouldBuildShip()

            if (Game.turnsLeft > 50) {
                defaultStrategy()
            } else {
                endGameSuicideStrategy()
            }

            Navigator().doNavigation()

            Game.me.ships.forEach { ship ->
                Log.log(ship.toString())
            }

            Game.endTurn()*/



            if (Game.turnsLeft > 60) {
                defaultStrategy()
            } else {
                endGameSuicideStrategy()
            }

            Navigator().doNavigation()

            Game.endTurn()

            Log.log("time: ${System.currentTimeMillis() - start}")
        }
    }
}
