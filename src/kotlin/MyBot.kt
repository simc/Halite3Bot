import Constants.SHIP_COST

object MyBot {

    fun checkShouldBuildShip() {
        val enoughHalite = Game.me.halite >= SHIP_COST
        val shipyardFree = !Game.me.shipyard.mapCell.hasShip || !Game.me.shipyard.mapCell.ship!!.isMine
        val noShipsWaiting = Game.me.shipyard.mapCell.reachableCells.none { it.ship?.onWayBack == true }
        //val enoughHaliteAvailable = Game.map.currentHalite.toDouble() / Game.map.totalHalite > 0.45
        val shouldBuild = Game.turnsLeft > 150
        if (enoughHalite && shipyardFree && shouldBuild && noShipsWaiting) {
            Game.map.buildShip()
            Log.log("Building ship")
        }
    }

    fun findBestLocationForShip(ship: Ship) {
        if (ship.id == -1) {
            return
        }
        if (!ship.canMove) {
            ship.dig()
            return
        }

        if (ship.onWayBack) {
            if (ship.halite > 500) {
                ship.targetDropoff()
                return
            } else {
                ship.onWayBack = false
            }
        }

        if (ship.isFull) { // A ship is considered full over 900
            ship.targetDropoff()

        } else {
            iterateByDistance(ship.position).forEach {
                if (it.reward > 20) {
                    if (it.isTargetEmpty || it.ship == ship) {
                        ship.targetPosition(it.position)
                        return
                    }
                }
            }
            ship.dig()
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
                ship.endGameSuicide = true
                if (!ship.canMove) {
                    ship.dig()
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
