import Constants.DROPOFF_COST
import Constants.SHIP_COST

object MyBot {

    fun checkShouldBuildShip() {
        if (Game.turnsLeft < 150) {
            return
        }

        //Is there enough halite for building a ship (and maybe a dropoff)
        val halite = Game.me.halite - DROPOFF_COST * if (Game.history.currentTurn.planBuildingDropOff) 1 else 0
        val enoughHalite = halite >= SHIP_COST

        //Can a ship on the shipyard leave for sure
        val shipyardCell = Game.me.shipyard.mapCell
        val shipyardDirectionFree = !shipyardCell.hasShip || shipyardCell.reachableCells.any { it.isEmpty }

        //If there are ships waiting near the shipyard, there is a ship built only every third frame
        val shipsWaiting = shipyardCell.reachableCells.any { it.ship?.isOnWayBack == true }
        val lastTurnsShipBuilt = Game.history.turns.takeLast(2).any(HistoryEntry::builtShip)
        val disableBuildShip = shipsWaiting && lastTurnsShipBuilt

        //val enoughHaliteAvailable = Game.map.currentHalite.toDouble() / Game.map.totalHalite > 0.45
        if (enoughHalite && shipyardDirectionFree && !disableBuildShip) {
            Game.sendCommand(Command.spawnShip())
            Game.history.currentTurn.builtShip = true
            shipyardCell.reserved = true
            Log.log("Building ship")
        }
    }

    fun checkShouldBuildDropoff() {

    }

    fun findBestLocationForShip(ship: Ship) {
        if (ship.id == -1) {
            return
        }

        if (!ship.canMove) {
            ship.navCharge()
            return
        }

        if (ship.isOnWayBack) {
            if (ship.halite > 500) {
                ship.targetDropoff()
                return
            } else {
                ship.task = Ship.Task.NONE
            }
        }

        if (ship.isFull) { // A ship is considered full over 900
            ship.task = Ship.Task.GOTO_DROPOFF
            ship.targetDropoff()

        } else {
            ship.task = Ship.Task.DIG

            iterateByDistance(ship.position).forEach {
                if (it.reward > 20) {
                    if (it.isTargetEmpty || it.ship == ship) {
                        ship.targetPosition(it.position)
                        return
                    }
                }
            }
            ship.navDig()
        }
    }

    fun createMapRating(ship: Ship): List<List<Int>> {
        var x = 0
        var y = 0
        return Game.map.cells.map {
            val inner = it.map {
                2
                y++
            }
            x++
            inner
        }
    }

    fun defaultStrategy() {
        Game.me.ships.forEach { ship ->
            findBestLocationForShip(ship)
        }
    }

    fun endGameSuicideStrategy() {
        val ships = Game.me.ships

        if (ships.isEmpty())
            return

        val shipToDropoffDistances = ships.map {
            val nextDropoff = nextDropoff(it.position)
            calculateDistance(it.position, nextDropoff.position)
        }

        if (shipToDropoffDistances.max()!! >= Game.turnsLeft) {
            ships.forEach { ship ->
                ship.task = Ship.Task.END_GAME_SUICIDE

                if (!ship.canMove) {
                    ship.targetDig()
                } else {
                    ship.targetDropoff()
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

            checkShouldBuildShip()

            if (Game.turnsLeft > 50) {
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
