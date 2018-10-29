import Constants.DROPOFF_COST
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
        val lastTurnsShipBuilt = Game.history.turns.takeLast(4).any(HistoryEntry::builtShip)
        val disableBuildShip = shipsWaiting && lastTurnsShipBuilt

        if (enoughHalite && shipyardDirectionFree && !disableBuildShip) {
            Game.sendCommand(Command.spawnShip())
            Game.history.currentTurn.builtShip = true
            shipyardCell.reserved = true
            Game.me.halite -= SHIP_COST
            Log.log("Building ship")
        }
    }

    private fun checkShouldBuildDropoff() {
        val fieldCells = haliteFields.flatten()
        val goodTargetsNearExistingDropoff = fieldCells.count { it.nextDropoffDistance < 8 }
        val notEnoughTargetsNearExistingDropoff = goodTargetsNearExistingDropoff < Game.me.ships.size
        val enoughShips = Game.me.ships.size > (Game.me.allDropoffs.size + 1) * 6
        if (notEnoughTargetsNearExistingDropoff && enoughShips) {
            val possibleDropoffs = findDropoffLocations()
            possibleDropoffs.forEach {
                Log.log("${it.first}: ${it.second}")
            }
            //val pos = possibleDropoffs.sortedByDescending { it.second }.take(1).map { it.first.position }
            //drawHaliteFields(haliteFields, false, pos)

            val newDropoff = possibleDropoffs.sortedByDescending {
                it.second
            }.firstOrNull()

            val plannedDropoff = Game.history.currentTurn.plannedDropoff
            if (plannedDropoff != null && possibleDropoffs.any { it.first.position == plannedDropoff }) {
                //drawHaliteFields(haliteFields, false, listOf(plannedDropoff))
                Game.history.currentTurn.reservedHalite = DROPOFF_COST - Game.map.at(plannedDropoff).halite
                findShipToBuildDropoff(plannedDropoff)
                return

            } else if (newDropoff != null) {
                //drawHaliteFields(haliteFields, false, listOf(newDropoff.first.position))
                Game.history.currentTurn.reservedHalite = DROPOFF_COST - newDropoff.first.halite
                findShipToBuildDropoff(newDropoff.first.position)
                return
            }
        }

        Game.history.currentTurn.reservedHalite = 0
        Game.me.ships.forEach {
            if (it.isBuildingDropoff) {
                it.task = Ship.Task.NONE
                Log.log("cancel dropoff")
            }
        }
    }

    private fun findDropoffLocations(): ArrayList<Pair<MapCell, Int>> {
        val possibleSpots = arrayListOf<Pair<MapCell, Int>>()
        for (x in 0 until Game.map.size) {
            for (y in 0 until Game.map.size) {
                val cell = Game.map.cells[x][y]
                if (cell.halite < 300) continue

                val spotHalite = cell.halite + iterateByDistance(cell.position, 0, 5).sumBy { it.halite }

                if (spotHalite < 15000) continue

                //Next drop off at least 15 away
                if (cell.nextDropoffDistance !in 14..25) continue

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

    private fun findShipToBuildDropoff(dropoff: Position) {
        Log.log("plan building dropoff ${Game.me.halite} at $dropoff")

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
        ship.target = dropoff
        if (ship.reachedTarget) {
            if (Game.me.halite >= ship.dropoffCost) {
                Game.history.currentTurn.plannedDropoff = null
                ship.navBuildDropoff()
                Game.me.halite -= ship.dropoffCost
                Log.log("build dropoff")

            } else {
                ship.navWait()
                Log.log("wait dropoff")
            }
        }

        Game.me.ships.forEach {
            if (it != ship && it.isBuildingDropoff) {
                it.task = Ship.Task.NONE
                Log.log("change dropoff ship")
            }
        }
    }

    private var haliteFields = arrayListOf<List<MapCell>>()

    private fun calculateHalieFields(): ArrayList<List<MapCell>> {
        val cells = Game.map.flatCells
        val average = Game.map.flatCells.sumBy { it.halite } / cells.size
        val threshold = (average * 1.7).toInt().coerceIn(100, 200)

        val visited = Array(Game.map.size) { BooleanArray(Game.map.size) }
        val fields = arrayListOf<List<MapCell>>()
        val stack = Stack<MapCell>()

        for (cell in cells) {
            if (visited[cell.position.x][cell.position.y])
                continue
            visited[cell.position.x][cell.position.y] = true

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

    fun targetHighRewardFields() {
        val averageReward = Game.map.flatCells.sumBy { it.reward } / Game.map.flatCells.size
        val highReward = Game.map.flatCells.filter {
            it.reward > averageReward * 15
        }
        //drawHaliteFields(haliteFields, false, highReward.map { it.position })
    }

    fun defaultStrategy() {
        targetHighRewardFields()

        val idleShips = Game.me.ships.filter { ship ->
            val hasTarget = validateExistingTarget(ship)
            !hasTarget
        }

        val fieldCells = haliteFields.flatten()
        val edges = fieldCells.filter { it.fieldEdges.count { it } > 0 }
        idleShips.forEach { ship ->
            findFieldEdgeTargetForShip(ship, edges)
        }
    }

    private fun validateExistingTarget(ship: Ship): Boolean {
        if (ship.isBuildingDropoff) {
            return true

        } else if ((ship.isOnWayBack && ship.halite > 500) || ship.isFull) {
            Log.log("ship #${ship.id} comes back")
            ship.task = Ship.Task.GOTO_DROPOFF
            ship.target = ship.mapCell.nextDropoff.position

            if (!ship.canMove)
                ship.navCharge()

            return true

        } else if (ship.task == Ship.Task.DIG) {
            if (ship.reachedTarget) {
                if (ship.mapCell.reward > 20) {
                    ship.navDig()
                    return true

                } else {
                    ship.task = Ship.Task.NONE
                }

            } else {
                if (Game.map.at(ship.target!!).ship?.isMine == false) {
                    ship.task = Ship.Task.NONE

                } else {
                    if (!ship.canMove)
                        ship.navCharge()
                    return true
                }
            }
        }

        return false
    }

    private fun findFieldEdgeTargetForShip(ship: Ship, edges: List<MapCell>) {
        val bestEdge = edges.filter {
            it.targetOf.isEmpty()
        }.minBy { edge ->
            calculateDistance(ship.position, edge.position) * 3 + edge.nextDropoffDistance * 2
        }

        if (bestEdge != null) {
            ship.task = Ship.Task.DIG
            ship.target = bestEdge.position

            if (!ship.canMove) {
                ship.navWait()
            }
            Log.log("NEW TARGET: $ship")
            if (ship.reachedTarget) {
                ship.navDig()
            }

        } else {
            findOtherTargetForShip(ship)
        }
    }

    private fun findOtherTargetForShip(ship: Ship) {
        ship.task = Ship.Task.DIG
        iterateByDistance(ship.position).forEach {
            if (it.reward > 20) {
                if (it.isTargetEmpty || it.ship == ship) {
                    ship.target = it.position
                    if (ship.reachedTarget)
                        ship.navDig()
                    return
                }
            }
        }
        ship.navDig()
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

            if (Game.turnsLeft > 60) {
                defaultStrategy()
            } else {
                endGameSuicideStrategy()
            }

            Navigator().doNavigation()

            Game.endTurn()

            if (Game.turnsLeft < 3 && Game.me.ships.size > 4) {
                //throw OutOfMemoryError("Why are there still ships?!")
            }

            Log.log("time: ${System.currentTimeMillis() - start}")
        }
    }
}
