data class Edge<T, E : Number>(val from: T, val to: T, val value: E)

interface Graph<T, E : Number> {
    fun getAllVertices(): Set<T>
    fun adjacentVertices(vertex: T): Set<T>
    fun getDistance(from: T, to: T): E
}

class GraphImpl<T, E : Number>(val directed: Boolean = false, val defaultCost: E): Graph<T, E> {
    private val edges = mutableSetOf<Edge<T, E>>()
    private val vertices = mutableSetOf<T>()

    fun addVertex(node: T): GraphImpl<T, E> {
        vertices += node
        return this
    }

    fun addArc(pair: Pair<T, T>, value: E? = null): GraphImpl<T, E> {
        val (from, to) = pair
        addVertex(from)
        addVertex(to)

        addRelation(from, to, value ?: defaultCost)
        if (!directed) {
            addRelation(to, from, value ?: defaultCost)
        }

        return this
    }

    override fun getAllVertices(): Set<T> {
        return vertices.toSet()
    }

    private fun addRelation(from: T, to: T, value: E) {
        edges.add(Edge(from, to, value))
    }

    override fun adjacentVertices(from: T): Set<T> {
        return edges.filter { it.from == from }.map { it.to }.toSet()
    }

    override fun getDistance(from: T, to: T): E {
        return edges
                .filter { it.from == from && it.to == to }
                .map { it.value }
                .first()
    }

    override fun toString(): String {
        return vertices.joinToString("\n") {
            val adjacentNodes = adjacentVertices(it)
            val connection = if (directed) "->" else "--"
            "$it $connection $adjacentNodes"
        }
    }
}

fun <T, E: Number> shortestPath(graph: Graph<T, E>, from: T, destination: T): Pair<List<T>, Double> {
    return dijkstra(graph, from, destination)[destination] ?: (emptyList<T>() to Double.POSITIVE_INFINITY)
}

private fun <T, E : Number> dijkstra(graph: Graph<T, E>, from: T, destination: T? = null): Map<T, Pair<List<T>, Double>> {
    val unvisitedSet = graph.getAllVertices().toMutableSet()
    val distances = graph.getAllVertices().map { it to Double.POSITIVE_INFINITY }.toMap().toMutableMap()
    val paths = mutableMapOf<T, List<T>>()
    distances[from] = 0.0

    var current = from

    while (unvisitedSet.isNotEmpty() && unvisitedSet.contains(destination)) {
        graph.adjacentVertices(current).forEach { adjacent ->
            val distance = graph.getDistance(current, adjacent).toDouble()
            if (distances[current]!! + distance < distances[adjacent]!!) {
                distances[adjacent] = distances[current]!! + distance
                paths[adjacent] = paths.getOrDefault(current, listOf(current)) + listOf(adjacent)
            }
        }

        unvisitedSet.remove(current)

        if (current == destination || unvisitedSet.all { distances[it]!!.isInfinite() }) {
            break
        }

        if (unvisitedSet.isNotEmpty()) {
            current = unvisitedSet.minBy { distances[it]!! }!!
        }
    }

    return paths.mapValues { entry -> entry.value to distances[entry.key]!! }
}