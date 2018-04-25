import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  //implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  //implicit val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  new Job().alertRiseAndFall andThen {
    case _ => Http.terminate()
  } onComplete {
    case Success(_) =>
    case Failure(t) => t.printStackTrace()
  }
}
