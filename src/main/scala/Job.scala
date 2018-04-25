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
}
