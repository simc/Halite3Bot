import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sign
import kotlin.random.Random

val availableSizes = listOf(32, 40, 48, 56, 64)
val processContext = newFixedThreadPoolContext(10, "runProcesses")
val rand = Random(System.currentTimeMillis())

fun main(args: Array<String>) {
    val gym = Gym()

    //gym.buildMyBot()

    File("replay/").listFiles().forEach { it.delete() }

    val start = System.currentTimeMillis()
    val jobs = availableSizes.flatMap { size ->
        (1..8).map {
            gym.runGame(false, size)
        }

        (1..8).map {
            gym.runGame(true, size)
        }
    }

    runBlocking {
        jobs.awaitAll()
    }

    val won = gym.wonCount.get()
    val gameCount = gym.gameCount.get()
    val winPercent = Math.round((won.toDouble() / gameCount) * 100)
    val averageExecTime = (System.currentTimeMillis() - start) / gameCount
    println("Won $winPercent% ($won/$gameCount)")
    println("Average Halite: ${gym.averageHalite} (Enemy: ${gym.averageEnemyHalite})")
    println("Average ExecTime: $averageExecTime ms")
}

class Gym {
    val gameCount = AtomicInteger(0)
    val wonCount = AtomicInteger(0)
    val lostCount = AtomicInteger(0)

    val totalHalite = AtomicInteger(0)
    val totalEnemyHalite = AtomicInteger(0)

    val averageHalite: Int
        get() = totalHalite.get() / gameCount.get()

    val averageEnemyHalite: Int
        get() = totalEnemyHalite.get() / gameCount.get()


    val regex = Regex("Player (\\d), '[\\w^']+', was rank (\\d) with (\\d+) halite")

    fun buildMyBot() {
        val process = ProcessBuilder(listOf("./gradlew", ":bot:submission")).start()
        process.waitFor()
    }

    fun runGame(fourPlayers: Boolean, size: Int) = GlobalScope.async(processContext) {
        gameCount.incrementAndGet()

        var seed = rand.nextInt()
        seed *= seed.sign

        val commands = arrayListOf(
                "../halite",
                "-vvv",
                "--no-logs",
                "-s",
                seed.toString(),
                "-n ${if (fourPlayers) 4 else 2}",
                "-o MyBot",
                "-o TestBot1"
        )

        if (fourPlayers) {
            commands += listOf("-o TestBot2", "-o TestBot3")
        }
        commands += listOf("--width $size", "--height $size", "java -jar ../bot/build/libs/MyBot.jar", "java -jar ../TestBot.jar")
        if (fourPlayers) {
            commands += listOf("java -jar ../TestBot.jar", "java -jar ../TestBot.jar")
        }

        //println(commands.joinToString(" "))

        val start = System.currentTimeMillis()
        val pb = ProcessBuilder(commands)
        pb.directory(File("replay/"))
        val process = pb.start()
        process.waitFor()

        var result = process.inputStream.bufferedReader().use { it.readText() }
        result += process.errorStream.bufferedReader().use { it.readText() }

        //println(result)

        if (result.contains("error")) {
            println(result)
            println("Error while running game")
            System.exit(1)
        }

        var enemyHalite = 0
        var count = 0
        regex.findAll(result).forEach {
            count++
            val player = it.groupValues[1].toInt()
            val halite = it.groupValues[3].toInt()
            if (player == 0) {
                val won = it.groupValues[2].toInt() == 1
                if (won) {
                    wonCount.incrementAndGet()
                } else {
                    lostCount.incrementAndGet()
                }
                totalHalite.addAndGet(halite)

                val time = System.currentTimeMillis() - start
                println("${if (won) "Won" else "Lost"}: size $size, 4p $fourPlayers ($time ms)")

            } else {
                enemyHalite += halite
            }
        }

        if (fourPlayers) {
            if (count != 4) {
                throw IllegalStateException("Game error")
            }
            totalEnemyHalite.addAndGet(enemyHalite / 3)
        } else {
            if (count != 2) {
                throw IllegalStateException("Game error")
            }
            totalEnemyHalite.addAndGet(enemyHalite)
        }
    }
}