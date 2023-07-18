import java.util.Scanner
import java.util.concurrent.{ExecutionException, ThreadLocalRandom}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main {
  final class GithubService() {
    private def getCommitsByUserAndRepo(user: String, repo: String) =
      repo match {
        case "org1/repo1" =>
          user match {
            case "user1" => 15
            case "user2" => 7
            case "user3" => 4
            case _       => 0
          }
        case "org1/repo2" =>
          user match {
            case "user1" => 5
            case "user2" => 9
            case "user3" => 15
            case _       => 0
          }
        case "org2/repo1" =>
          user match {
            case "user1" => 10
            case "user2" => 5
            case "user3" => 6
            case _       => 0
          }
        case "org2/repo2" =>
          user match {
            case "user1" => 15
            case "user2" => 3
            case "user3" => 13
            case _       => 0
          }
        case "org2/repo3" =>
          user match {
            case "user1" => 4
            case "user2" => 8
            case "user3" => 7
            case _       => 0
          }
        case _ => throw new IllegalArgumentException("Unknown repo: " + repo)
      }

    def commitCountSync(repo: String, user: String): Int =
      getCommitsByUserAndRepo(user, repo)

    def commitCountSyncWithUser(repo: String, user: String): (String, Int) =
      user -> getCommitsByUserAndRepo(user, repo)

    def commitCount(repo: String, user: String): Future[Int] =
      Future {
        Thread.sleep(ThreadLocalRandom.current().nextInt(1000))
        getCommitsByUserAndRepo(user, repo)
      }

    def commitCountWithUser(repo: String, user: String): Future[(String, Int)] =
        commitCount(repo, user).map(user -> _)
  }

  private val ALL_VALID: List[(String, String)] = List(
    ("org1/repo1", "user2"),
    ("org1/repo2", "user3"),
    ("org2/repo1", "user1"),
    ("org2/repo2", "user2"),
    ("org2/repo3", "user2")
  )

  private val SOME_INVALID: List[(String, String)] = List(
    ("org1/repo1", "user2"),
    ("org1/repo2", "user3"),
    ("org2/repo1", "user1"),
    ("org2/repo2", "user2"),
    ("not-a-valid-repo", "user4")
  )

  private val githubService = new GithubService

  def main(args: Array[String]): Unit = {
    val answer = readInput match {
      case "Exercise0" => Future.successful(exercise0(ALL_VALID))
      case "Exercise1" => exercise1(ALL_VALID)
      case "Exercise2" => exercise2(SOME_INVALID, "user2")
      case "Exercise3" => exercise3(SOME_INVALID).map(adjustSerialization)
      case _           => throw new IllegalArgumentException("Unknown exercise: " + args(0))
    }

    println(Await.result(answer, Duration.Inf))
  }

  def readInput: String = {
    val scanner = new Scanner(System.in)
    val input = scanner.next
    scanner.close()
    input
  }

  private def adjustSerialization(answer: Map[String, Int]): String =
    answer.map { case (k, v) => s"$k: $v" }.mkString(", ")

  private def exercise0(userList: List[(String, String)]): Int =
    userList.map { case (repo, user) => githubService.commitCountSync(repo, user) }.sum

  private def exercise1(userList: List[(String, String)]): Future[Int] = {
    // Exercise 1: Get the total number of commits for users in all repos. Assume the happy path.
    Future
      .traverse(userList) { case (repo, user) =>
        githubService.commitCount(repo, user)
      }
      .map(_.sum)
  }

  private def exercise2(userList: List[(String, String)], user: String): Future[Int] = {
    // Exercise 2: Get the total number of commits for a given user in all repos, but ignore any repos that don't exist
    Future
      .traverse(userList.filter { case (_, u) => user == u }) { case (repo, user) =>
        githubService
          .commitCount(repo, user)
          .recover { case _: Exception => 0 }
      }
      .map(_.sum)
  }

  private def exercise3(userList: List[(String, String)]): Future[Map[String, Int]] = {
    // Exercise 3: Get the total number of commits for each user for all repos
    Future
      .traverse(userList) { case (repo, user) =>
        githubService
          .commitCountWithUser(repo, user)
          .recover { case _: Exception => (user, 0) }
      }
      .map(_.groupMapReduce(_._1)(_._2)(_ + _))
  }

}
