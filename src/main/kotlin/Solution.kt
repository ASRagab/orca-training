import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import kotlin.random.Random


private val ALL_VALID = listOf(Pair("org1/repo1", "user2"), Pair("org1/repo2", "user3"), Pair("org2/repo1", "user1"), Pair("org2/repo2", "user2"), Pair("org2/repo3", "user2"))

private val SOME_INVALID = listOf(Pair("org1/repo1", "user2"), Pair("org1/repo2", "user3"), Pair("org2/repo1", "user1"), Pair("org2/repo2", "user2"), Pair("not-a-valid-repo", "user4"))

fun readInput(): String? {
    val scanner = Scanner(System.`in`)
    val input = scanner.next()
    scanner.close()
    return input
}

class GithubService {
    private fun getCommitsByUserAndRepo(user: String, repo: String): Int {
        return when (repo) {
            "org1/repo1" -> when (user) {
                "user1" -> 15
                "user2" -> 7
                "user3" -> 4
                else -> 0
            }

            "org1/repo2" -> when (user) {
                "user1" -> 5
                "user2" -> 9
                "user3" -> 15
                else -> 0
            }

            "org2/repo1" -> when (user) {
                "user1" -> 10
                "user2" -> 5
                "user3" -> 6
                else -> 0
            }

            "org2/repo2" -> when (user) {
                "user1" -> 15
                "user2" -> 3
                "user3" -> 13
                else -> 0
            }

            "org2/repo3" -> when (user) {
                "user1" -> 4
                "user2" -> 8
                "user3" -> 7
                else -> 0
            }

            else -> throw IllegalArgumentException("Unknown repo: $repo")
        }
    }

    fun commitCountSync(repo: String, user: String): Int = getCommitsByUserAndRepo(user, repo)
    fun commitCountSyncWithUser(repo: String, user: String): Pair<String, Int> = Pair(user, getCommitsByUserAndRepo(user, repo))


    fun commitCount(repo: String, user: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                Thread.sleep(Random.nextLong(1000))
                getCommitsByUserAndRepo(user, repo)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

        }
    }

    fun commitCountWithUser(repo: String, user: String): CompletableFuture<Pair<String, Int>> {
        return CompletableFuture.supplyAsync {
            try {
                Thread.sleep(Random.nextLong(1000))
                Pair(user, getCommitsByUserAndRepo(user, repo))
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: IllegalArgumentException) {
                throw RuntimeException(e)
            }
        }
    }
}

val service = GithubService()

fun main(args: Array<String>) {
    println(when (readInput()) {
        "Exercise0" -> exercise0(ALL_VALID)
        "Exercise1" -> exercise1(ALL_VALID)
        "Exercise2" -> exercise2(SOME_INVALID, "user2")
        "Exercise3" -> adjustSerialization(exercise3(SOME_INVALID))
        else -> throw IllegalArgumentException("Unknown exercise: " + args[0])
    })
}

fun adjustSerialization(answer: Map<String, Int>): String {
    return answer.entries.sortedBy { it.key }
            .stream()
            .map { entry -> entry.key + ": " + entry.value }
            .collect(Collectors.joining(", "))
}

fun exercise0(user: List<Pair<String, String>>): Int =
        user.stream()
                .map { (repo, user) -> service.commitCountSync(repo, user) }
                .collect(Collectors.toList())
                .sum()

fun exercise1(userList: List<Pair<String, String>>): Int =
        userList.parallelStream()
                .map { (repo, user) -> service.commitCount(repo, user) }
                .map { it.join() }
                .collect(Collectors.toList())
                .sum()


fun exercise2(userList: List<Pair<String, String>>, user: String): Int =
        userList.filter { it.second == user }
                .parallelStream()
                .map { (repo, user) -> service.commitCount(repo, user).exceptionally { 0 } }
                .map { it.join() }
                .collect(Collectors.toList())
                .sum()


fun exercise3(userList: List<Pair<String, String>>): Map<String, Int> =
        userList.parallelStream()
                .map { (repo, user) -> service.commitCountWithUser(repo, user).exceptionally { Pair(user, 0) } }
                .map { it.join() }
                .collect(Collectors.toList())
                .groupBy { it.first }
                .mapValues { (_, values) -> values.sumBy { it.second } }


