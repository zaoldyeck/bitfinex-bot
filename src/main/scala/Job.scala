import play.api.libs.ws.StandaloneWSResponse

import scala.concurrent.{ExecutionContext, Future}

class Job(implicit ec: ExecutionContext) {
  private val task = new Task()
  private val bitfinexAPI = new BitfinexAPI()

  def findOutGoodTarget: Future[StandaloneWSResponse] = {
    (for {
      symbols <- bitfinexAPI.getAllSymbols
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
      symbols <- bitfinexAPI.getAllSymbols
      candles <- task.getAllSymbolCandles(symbols)
    } yield {
      candles.sortBy(-_.usdVolume).foreach(c => println(c.symbol + "," + c.usdVolume))
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

  def LendingUSD: Future[Unit] = {
    for {
      wallets <- bitfinexAPI.getWallets
      used <- bitfinexAPI.getSumOfFundingCredits()
      unused <- Future(wallets.filter(wallet => wallet.walletType == "funding" && wallet.currency == "USD").map(_.balance).sum - used)
      activeHighestRate <- bitfinexAPI.getHighestRateOfActiveFundingOffers()
      highestRate <- bitfinexAPI.getHistCandle("fUSD", "1m", ":p2", 60).map(candles => candles.map(_.high).max)
      result <- if (unused >= 50 && activeHighestRate != highestRate) bitfinexAPI.cancelAllFundingOffers().flatMap(_ => bitfinexAPI.submitFundingOffer(unused, highestRate)) else Future("Do nothing")
    } yield {
      println(result)
    }
  }
}




