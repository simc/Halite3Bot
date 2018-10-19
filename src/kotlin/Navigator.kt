import java.lang.IllegalStateException

class Navigator {
    fun doNavigation() {
        for (ship in Game.me.ships) {
            if (ship.id == -1) {
                Game.sendCommand(Command.spawnShip())
                return

            } else {
                navigateShip(ship)
            }
        }
    }

    private fun navigateShip(ship: Ship) {
        if (ship.navigationFinished)
            return

        if (ship.target == null)
            throw IllegalStateException("Target of ship #${ship.id} is null")

        if (ship.target == ship.mapCell)
            return

        val moves = Game.map.getUnsafeMoves(ship.position, ship.target!!.position).shuffled()

        //Find possible swaps in optimal direction
        for (move in moves) {
            if (trySwapInDirection(ship, move)) {
                return
            }
        }

        //Try to move in optimal direction
        for (move in moves) {
            if (tryMoveInDirection(ship, move)) {
                return
            }
        }

        //Try to navigate ships which block optimal directions
        for (move in moves) {
            if (tryMoveInDirection(ship, move, true)) {
                return
            }
        }

        //Move in other direction if not near a dropoff
        if (moves.size < 4) {
            if (!ship.onWayBack || calculateDistance(ship.position, nextDropoff(ship.position).position) > 2) {
                val movesLeft = (Direction.ALL_CARDINALS - moves).shuffled()
                for (move in movesLeft) {
                    if (tryMoveInDirection(ship, move)) {
                        return
                    }
                }
            }
        }
    }

    private fun sendMove(ship: Ship, direction: Direction) {
        Game.sendCommand(Command.move(ship.id, direction))
        ship.navigationFinished = true
    }

    private fun trySwapInDirection(ship: Ship, direction: Direction): Boolean {
        val newPosition = ship.position.directionalOffset(direction)
        if (!isAllowedOnPosition(ship, newPosition)) {
            return false
        }

        val newCell = Game.map.at(newPosition)
        val otherShip = newCell.ship
        if (otherShip != null && otherShip.isMine && !otherShip.navigationFinished) {
            val otherShipMoves = Game.map.getUnsafeMoves(otherShip.position, otherShip.target!!.position)
            if (otherShipMoves.contains(direction.invertDirection())) {
                otherShip.mapCell.ship = ship
                ship.mapCell.ship = otherShip

                otherShip.position = ship.position
                ship.position = newPosition

                sendMove(ship, direction)
                sendMove(otherShip, direction.invertDirection())

                return true
            }
        }

        return false
    }

    private fun tryMoveInDirection(ship: Ship, direction: Direction, navigateBlockingShip: Boolean = false): Boolean {
        val newPosition = ship.position.directionalOffset(direction)
        if (!isAllowedOnPosition(ship, newPosition)) {
            return false
        }

        val cell = Game.map.at(newPosition)
        if (cell.hasShip) {
            val otherShip = cell.ship!!
            if (otherShip.isMine) {
                if (ship.endGameSuicide && otherShip.endGameSuicide && cell.structure?.isMine == true) {
                    moveInDirection(ship, direction)
                    return true

                } else if (navigateBlockingShip) {
                    navigateShip(otherShip)
                    return tryMoveInDirection(ship, direction)
                }

            } else if (cell.structure?.isMine == true) {
                moveInDirection(ship, direction)
                return true
            }

        } else if (cell.structure?.isMine != false) { //Dont go over enemy dropoffs
            moveInDirection(ship, direction)
            return true
        }

        return false
    }

    private fun moveInDirection(ship: Ship, direction: Direction) {
        ship.mapCell.ship = null
        ship.position = ship.position.directionalOffset(direction)
        ship.mapCell.ship = ship

        sendMove(ship, direction)
    }

    private fun isAllowedOnPosition(ship: Ship, position: Position): Boolean {
        val isDropoff = Game.map.at(position).structure?.isMine == true
        return !isDropoff || ship.onWayBack || ship.endGameSuicide
    }
}