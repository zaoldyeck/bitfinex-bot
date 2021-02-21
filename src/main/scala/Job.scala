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
      totalFunding <- bitfinexAPI.getWallets.map(wallets => wallets.filter(wallet => wallet.walletType == "funding" && wallet.currency == "USD").map(_.balance).sum)
      used <- bitfinexAPI.getSumOfFundingCredits()
      activeHighestRate <- bitfinexAPI.getHighestRateOfActiveFundingOffers()
      highestRate <- bitfinexAPI.getHistCandle("fUSD", "1m", ":p2", 60).map(candles => candles.map(_.high).max)
      result <- {
        val unused = totalFunding - used
        if (unused >= 50 && activeHighestRate != highestRate) {
          val expectedAmount = totalFunding / 12
          val amount = if (unused >= expectedAmount) expectedAmount else unused
          val period = highestRate match {
            case rate if rate > 1 => 120
            case _ => 2
          }
          bitfinexAPI.cancelAllFundingOffers().flatMap(_ => bitfinexAPI.submitFundingOffer(amount, highestRate, period = period))
        } else Future("Do nothing")
      }
    } yield {
      println(result)
    }

    /*
    for {
      wallets <- bitfinexAPI.getWallets
      used <- bitfinexAPI.getSumOfFundingCredits()
      activeHighestRate <- bitfinexAPI.getHighestRateOfActiveFundingOffers()
      highestRate <- bitfinexAPI.getHistCandle("fUSD", "1m", ":p2", 60).map(candles => candles.map(_.high).max)
      result <- {
        val unused = wallets.filter(wallet => wallet.walletType == "funding" && wallet.currency == "USD").map(_.balance).sum - used
        if (unused >= 50 && activeHighestRate != highestRate) {
          val period = highestRate match {
            case rate if rate > 1 => 120
            case _ => 2
          }
          bitfinexAPI.cancelAllFundingOffers().flatMap(_ => bitfinexAPI.submitFundingOffer(unused, highestRate, period = period))
        } else Future("Do nothing")
      }
    } yield {
      println(result)
    }
     */
  }
}




