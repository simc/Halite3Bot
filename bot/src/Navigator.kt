import java.lang.IllegalStateException

class Navigator {
    fun doNavigation() {
        //Move ship on shipyard first
        if (Game.me.shipyard.mapCell.ship?.isMine == true) {
            navigateShip(Game.me.shipyard.mapCell.ship!!)
        }

        for (ship in Game.me.ships) {
            navigateShip(ship)
        }
    }

    private fun navigateShip(ship: Ship, moveBlockingShips: Boolean = true) {
        if (ship.hasNavigation) {
            if (!ship.commandSent) {
                sendNavigation(ship)
            }
            return
        }

        if (ship.target == null)
            throw IllegalStateException("Target is null: $ship")

        if (ship.task == Ship.Task.NONE)
            throw IllegalStateException("Task is not set: $ship")

        if (ship.reachedTarget) {
            Log.log(ship.toString())
            throw IllegalStateException("Ship arrived with no command: $ship")
        }

        val moves = Game.map.getUnsafeMoves(ship.position, ship.target!!).shuffled()

        //Find possible swaps in optimal direction
        for (move in moves) {
            if (trySwapInDirection(ship, move)) {
                return
            }
        }

        //Try to move in optimal direction
        val movesSortedByDangerAndCost = moves.sortedWith(compareBy ({
            if (ship.halite > 600) {
                Game.map.at(ship.position.directionalOffset(it)).reachableCells.count { cell ->
                    cell.ship?.isMine == false
                }
            } else {
                0
            }
        },  {
            Game.map.at(ship.position.directionalOffset(it)).halite
        }))
        for (move in movesSortedByDangerAndCost) {
            if (tryMoveInDirection(ship, move)) {
                return
            }
        }

        //Try to navigate ships which block optimal directions
        if (moveBlockingShips) {
            for (move in moves) {
                if (tryMoveInDirection(ship, move, true)) {
                    return
                }
            }
        }

        //Move in other direction if not near a dropoff
        if (moves.size < 4) {
            val movesLeft = (Direction.ALL_CARDINALS - moves).shuffled().toMutableList()
            //for (move in movesLeft) {
                //if (tryMoveInDirection(ship, move, true)) {
                    //return
                //}
            //}
            if (!ship.isOnWayBack || ship.mapCell.nextDropoffDistance > 2) {
                val movesLeft = (Direction.ALL_CARDINALS - moves).shuffled().toMutableList()
                val lastMove = Game.history.lastTurn?.getMove(ship)

                //Try to move in the same direction as last turn
                if (lastMove != null && movesLeft.remove(lastMove)) {
                    if (tryMoveInDirection(ship, lastMove)) {
                        return
                    }
                }

                //Is a move in inverted direction possible?
                val oppositeMove = if (lastMove != null) movesLeft.remove(lastMove.invertDirection()) else false

                val awayMoves = moves.mapNotNull {
                    val direction = it.invertDirection()
                    if (movesLeft.remove(direction))
                        direction
                    else
                        null
                }

                //Try all left moves
                for (move in movesLeft) {
                    if (tryMoveInDirection(ship, move)) {
                        return
                    }
                }

                //Try to move away from blocking ship
                for (move in awayMoves) {
                    if (tryMoveInDirection(ship, move)) {
                        return
                    }
                }

                // As a last resort, move one step back
                if (oppositeMove) {
                    if (tryMoveInDirection(ship, lastMove!!.invertDirection())) {
                        return
                    }
                }
            }
        }

        //Do nothing with ship
        ship.navWait()
        sendNavigation(ship)
    }

    private fun trySwapInDirection(ship: Ship, direction: Direction): Boolean {
        val newPosition = ship.position.directionalOffset(direction)
        if (!isAllowedOnPosition(ship, newPosition)) {
            return false
        }

        val newCell = Game.map.at(newPosition)
        val otherShip = newCell.ship
        if (otherShip != null && otherShip.isMine && !otherShip.hasNavigation) {
            val otherShipMoves = Game.map.getUnsafeMoves(otherShip.position, otherShip.target!!)
            val otherShipSwapGood = otherShipMoves.contains(direction.invertDirection())
            val otherShipPositionAllowed = isAllowedOnPosition(otherShip, ship.position)
            if (otherShipSwapGood && otherShipPositionAllowed) {
                otherShip.position = ship.position
                ship.position = newPosition

                ship.navMove(direction)
                sendNavigation(ship)

                otherShip.navMove(direction.invertDirection())
                sendNavigation(otherShip)

                return true
            }
        }

        return false
    }

    private fun tryMoveInDirection(ship: Ship, direction: Direction, moveBlockingShip: Boolean = false): Boolean {
        fun performMove() {
            ship.position = ship.position.directionalOffset(direction)

            ship.navMove(direction)
            sendNavigation(ship)
        }

        val newPosition = ship.position.directionalOffset(direction)
        if (!isAllowedOnPosition(ship, newPosition)) {
            return false
        }

        val cell = Game.map.at(newPosition)
        if (cell.hasShip) {
            val otherShip = cell.ship!!
            if (otherShip.isMine) {
                if (ship.isEndGameSuicide && otherShip.isEndGameSuicide && cell.structure?.isMine == true) {
                    performMove()
                    return true

                } else if (moveBlockingShip && !otherShip.hasNavigation) {
                    navigateShip(otherShip, moveBlockingShips = false)
                    return tryMoveInDirection(ship, direction)
                }

            } else if (cell.structure?.isMine == true) {
                performMove()
                return true
            }

        } else if (cell.structure?.isMine != false) { //Don't go over enemy dropoffs
            performMove()
            return true
        }

        return false
    }

    private fun isAllowedOnPosition(ship: Ship, position: Position): Boolean {
        val cell = Game.map.at(position)
        val isDropoff = cell.structure?.isMine == true
        val noFullShipNearby = iterateByDistance(position, 2).none { it.ship?.isOnWayBack == true }
        return (!isDropoff || ship.isOnWayBack || ship.isEndGameSuicide || noFullShipNearby) && !cell.reserved
    }

    private fun sendNavigation(ship: Ship) {
        if (ship.navAction == Ship.NavAction.BUILD_DROPOFF) {
            Game.sendCommand(Command.transformShipIntoDropoffSite(ship.id))
            ship.commandSent = true
            Game.history.currentTurn.doMove(ship, ship.navDirection!!)
        } else {
            Game.sendCommand(Command.move(ship.id, ship.navDirection!!))
            ship.commandSent = true
            Game.history.currentTurn.doMove(ship, ship.navDirection!!)
        }
    }
}