import Constants.DROPOFF_COST
import Constants.SHIP_COST

object MyBot {

    private fun checkShouldBuildShip() {
        if (Game.turnsLeft < 150) {
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

        Log.log("current: ${Game.map.currentHalite}, total: ${Game.map.totalHalite}, ratio: ${Game.map.currentHalite.toDouble() / Game.map.totalHalite}")
        val enoughHaliteAvailable = Game.map.currentHalite.toDouble() / Game.map.totalHalite > 0.55
        if (enoughHalite && shipyardDirectionFree && !disableBuildShip && enoughHaliteAvailable) {
            Game.sendCommand(Command.spawnShip())
            Game.history.currentTurn.builtShip = true
            shipyardCell.reserved = true
            Game.me.halite -= SHIP_COST
            Log.log("Building ship")
        }
    }

    private fun findDropOffLocations(): ArrayList<Pair<MapCell, Int>> {
        val possibleSpots = arrayListOf<Pair<MapCell, Int>>()
        for (x in 0 until Game.map.size) {
            for (y in 0 until Game.map.size) {
                val cell = Game.map.cells[x][y]
                if (cell.halite < 300) continue

                val spotHalite = cell.halite + iterateByDistance(cell.position, 0, 4).sumBy { it.halite }

                if (spotHalite < 15000) continue

                //Next drop off at least 15 away
                if (cell.nextDropOffDistance < 15) continue

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

    private fun checkShouldBuildDropOff() {
        if (Game.me.ships.size.toDouble() / Game.me.allDropoffs.size < 12) {
            Game.me.ships.forEach {
                if (it.isBuildingDropOff) {
                    it.task = Ship.Task.NONE
                    Log.log("cancel dropoff")
                }
            }
            return
        }

        val possibleDropOffs = findDropOffLocations().filter {
            it.first.nextDropOffDistance < 25
        }
        val newDropOff = possibleDropOffs.sortedBy {
            it.second
        }.firstOrNull()

        val plannedDropOff = Game.history.currentTurn.plannedDropOff
        if (plannedDropOff != null && possibleDropOffs.any { it.first.position == plannedDropOff }) {
            Game.history.currentTurn.reservedHalite = DROPOFF_COST - Game.map.at(plannedDropOff).halite
            findShipToBuildDropOff(plannedDropOff)

        } else if (newDropOff != null) {
            Game.history.currentTurn.reservedHalite = DROPOFF_COST - newDropOff.first.halite
            findShipToBuildDropOff(newDropOff.first.position)

        } else {
            Game.history.currentTurn.reservedHalite = 0
            Game.me.ships.forEach {
                if (it.isBuildingDropOff) {
                    it.task = Ship.Task.NONE
                    Log.log("cancel dropoff")
                }
            }
        }
    }

    private fun findShipToBuildDropOff(dropOff: Position) {
        Log.log("plan building dropoff ${Game.me.halite}")

        Game.history.currentTurn.plannedDropOff = dropOff
        iterateByDistance(dropOff).forEach {
            val ship = it.ship
            if (ship?.isMine == true) {
                buildDropOff(dropOff, ship)
                return
            }
        }
    }

    private fun buildDropOff(dropOff: Position, ship: Ship) {
        ship.task = Ship.Task.BUILD_DROP_OFF
        if (ship.targetPosition(dropOff)) {
            if (Game.me.halite >= ship.dropOffCost) {
                Game.history.currentTurn.plannedDropOff = null
                ship.navBuildDropOff()
                Game.me.halite -= ship.dropOffCost
                Log.log("build dropoff")

            } else {
                ship.navWait()
                Log.log("wait dropoff")
            }

        } else {
            Log.log("move dropoff")
        }

        Game.me.ships.forEach {
            if (it != ship && it.isBuildingDropOff) {
                it.task = Ship.Task.NONE
                Log.log("change dropoff ship")
            }
        }
    }

    private fun defaultStrategy() {
        val idleShips = arrayListOf<Ship>()

        for (ship in Game.me.ships) {
            if (ship.isBuildingDropOff) {
                continue

            } else if ((ship.isOnWayBack && ship.halite > 500) || ship.isFull) {
                ship.task = Ship.Task.GOTO_DROP_OFF
                ship.targetDropOff()

            } else if (ship.mapCell.reward > 20) {
                ship.task = Ship.Task.DIG
                ship.navDig()

            } else {
                ship.task = Ship.Task.NONE
                idleShips.add(ship)
            }
        }

        findTasksForShips(idleShips)

        Game.me.ships.forEach { ship ->
            if (!ship.canMove) {
                ship.navCharge()
            }
        }
    }


    private fun findTasksForShips(idleShips: ArrayList<Ship>) {
        val cells = Game.map.cells.flatten()
        val locations = cells.filter {
            it.halite - it.nextDropOffDistance * 15  > 0
        }.sortedByDescending {
            it.halite - it.nextDropOffDistance * 30
        }.take(idleShips.size)

        for (location in locations) {
            val ship = idleShips.minBy {
                calculateDistance(it.position, location.position)
            }!!
            ship.task = Ship.Task.DIG
            if (ship.targetPosition(location.position)) {
                ship.navDig()
            }
            idleShips.remove(ship)
        }

        shipLoop@
        for (ship in idleShips) {
            ship.task = Ship.Task.DIG
            for (cell in iterateByDistance(ship.position)) {
                if (cell.reward > 35) {
                    if (cell.isTargetEmpty || cell.ship == ship) {
                        if (ship.targetPosition(cell.position)) {
                            ship.navDig()
                        }
                        continue@shipLoop
                    }
                }
            }
            ship.navDig()
        }
    }


    private fun endGameSuicideStrategy() {
        val ships = Game.me.ships

        if (ships.isEmpty())
            return

        val (importantShips, otherShips) = ships.partition {
            it.halite > 0
        }
        val shipToDropOffDistances = importantShips.map {
            it.mapCell.nextDropOffDistance
        }

        if (importantShips.isEmpty() || shipToDropOffDistances.max()!! >= Game.turnsLeft) {
            importantShips.forEach { ship ->
                ship.task = Ship.Task.END_GAME_SUICIDE

                if (!ship.canMove) {
                    ship.navCharge()
                } else {
                    if (ship.targetDropOff()) {
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
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Game.init()
        Game.ready("leisim")

        Log.log("Successfully created bot! My Player ID is ${Game.myId}.")

        while (true) {
            val start = System.currentTimeMillis()
            Game.updateFrame()

            checkShouldBuildDropOff()
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

            Game.endTurn()

            Log.log("time: ${System.currentTimeMillis() - start}")
        }
    }
}
