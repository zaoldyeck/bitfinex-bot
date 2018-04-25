import play.api.libs.ws.StandaloneWSResponse

import scala.concurrent.{ExecutionContext, Future}

class Job(implicit ec: ExecutionContext) {
  private val task = new Task()
  private val bitfinexFetcher = new BitfinexFetcher()

  def findOutGoodTarget: Future[StandaloneWSResponse] = {
    (for {
      symbols <- bitfinexFetcher.getAllSymbols
      candles <- task.getAllSymbolCandles(symbols)
    } yield {
      val strongSymbols = task.filterStrongSymbols(candles)
      val weakSymbols = task.filterWeakSymbols(symbols, candles)
      s"""
         |Strong Symbols:
         | Symbol,    Rate,   Change,       Compare
         |${strongSymbols.mkString("\n")}
         |\n
         |Weak Symbols:
         | Symbol,    Rate,   Change,       Compare
         |${weakSymbols.mkString("\n")}
        """.stripMargin
    }).flatMap(new SlackBot().sendMessage(_))
  }

  def alertRiseAndFall: Future[Any] = {
    (for {
      symbols <- bitfinexFetcher.getAllSymbols
      candles <- task.getAllSymbolCandles(symbols)
    } yield {
      val risingSymbols = task.filterRisingSymbols(candles)
      val fallingSymbols = task.filterFallingSymbols(symbols, candles)
      if (risingSymbols.isEmpty && fallingSymbols.isEmpty) None else
        Some(
          s"""
             |<!everyone>
             |Rising Symbols:
             | Symbol,    Rate,   Change
             |${risingSymbols.mkString("\n")}
             |\n
             |Falling Symbols:
             | Symbol,    Rate,   Change
             |${fallingSymbols.mkString("\n")}
        """.stripMargin)
    }).flatMap {
      case Some(m) => new SlackBot().sendMessage(m)
      case None => Future.unit
    }
  }
}
