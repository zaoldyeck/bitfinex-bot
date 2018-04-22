import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  //implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  //implicit val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  private val job = new Job()
  job.getAllSymbolsCandle.flatMap {
    candles =>
      val strongSymbols = job.filterStrongSymbols(candles)
      val weakSymbols = job.filterWeakSymbols(candles)

      val message =
        s"""
           |Strong Symbols:
           | Symbol,    Rate,   Change,       Compare
           |${strongSymbols.mkString("\n")}
           |\n
           |Weak Symbols:
           | Symbol,    Rate,   Change,       Compare
           |${weakSymbols.mkString("\n")}
        """.stripMargin
      new SlackBot().sendMessage(message)
  } andThen {
    case _ => Http.terminate()
  } onComplete {
    case Success(_) =>
    case Failure(t) => t.printStackTrace()
  }
}
