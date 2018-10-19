import Constants.SHIP_COST

object MyBot {

    fun checkShouldBuildShip() {
        val enoughHalite = Game.me.halite >= SHIP_COST
        val shipyardFree = !Game.me.shipyard.mapCell.hasShip || !Game.me.shipyard.mapCell.ship!!.isMine
        val noShipsWaiting = Game.me.shipyard.mapCell.reachableCells.none { it.ship?.onWayBack == true }
        val enoughHaliteAvailable = Game.map.currentHalite.toDouble() / Game.map.totalHalite > 0.45
        if (enoughHalite && shipyardFree && enoughHaliteAvailable && noShipsWaiting) {
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
                if (it.reward > 30) {
                    if (it.isTargetEmpty || it.ship == ship) {
                        ship.targetPosition(it.position)
                        return
                    }
                }
            }
            ship.dig()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Game.init()
        Game.ready("BloodWork")

        Log.log("Successfully created bot! My Player ID is ${Game.myId}.")

        while (true) {
            val start = System.currentTimeMillis()
            Game.updateFrame()

            checkShouldBuildShip()
            Game.me.ships.forEach { ship ->
                findBestLocationForShip(ship)
                if (ship.target == null && ship.id != -1) {
                    Log.log("")
                }
            }

            Navigator().doNavigation()

            Game.endTurn()

            Log.log("time: ${System.currentTimeMillis() - start}")
        }
    }
}
