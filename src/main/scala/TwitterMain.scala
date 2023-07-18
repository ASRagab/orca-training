import com.twitter.util.{Duration, Future, JavaTimer}

object TwitterMain {
  implicit val timer = TwitterTimer.defaultTimer

  private def future1 = {
    println(s"future1 before: ${System.currentTimeMillis()}")
    Future.sleep(Duration.fromSeconds(1)).map { _ => println(s"future1: ${System.currentTimeMillis()}") }
  }

  private def future2: Future[Unit] = {
    println(s"future2 before: ${System.currentTimeMillis()}")
    Future.sleep(Duration.fromSeconds(2)).map { _ => println(s"future2: ${System.currentTimeMillis()}") }
  }

  def main(args: Array[String]): Unit = {
    println(s"main:    ${System.currentTimeMillis()}")
    val fut1 = future1
    val fut2 = future2
    for {
      _ <- fut1
      _ <- fut2
    } yield ()

    Thread.sleep(3000)
  }

}

object TwitterTimer {
  val defaultTimer = new JavaTimer(true)
}
