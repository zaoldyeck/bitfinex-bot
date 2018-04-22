import Extension._
import scala.concurrent.{ExecutionContext, Future}

class Job(implicit ec: ExecutionContext) {
  private val bitfinexFetcher = new BitfinexFetcher

  def getAllSymbolsCandle: Future[List[Candle]] = {
    bitfinexFetcher.getAllSymbols.flatMap {
      symbols =>
        Future.sequence(symbols
          .filter(_.endsWith("usd"))
          .map("t" + _.toUpperCase)
          .map(s => bitfinexFetcher.getLastCandle(s, timeFrame = "6h")))

      /*
      val usdSymbols = symbols.filter(_.endsWith("usd")).map("t" + _.toUpperCase)
      Future.sequence(usdSymbols.grouped(15).map {
        symbols =>
          Thread.sleep(5000)
          symbols.map(s => bitfinexFetcher.getLastCandle(s,timeFrame = "3h"))
      } reduce (_ ::: _))
      */
    }
  }

  def filterStrongSymbols(candles: List[Candle]): List[Analysis] = {
    val btcusd = candles.filter(_.symbol == "tBTCUSD").head
    val rate = btcusd.rate
    Analysis(btcusd.symbol, btcusd.rate.formatRate, btcusd.change.format, 0.formatRate) ::
      candles.sortBy(-_.usdVolume)
        .take(candles.size / 3 * 2)
        .filter(_.rate > rate)
        .sortBy(-_.rate).take(10)
        .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, (c.rate - rate).formatRate))
  }

  def filterWeakSymbols(candles: List[Candle]): List[Analysis] = {
    val btcusd = candles.filter(_.symbol == "tBTCUSD").head
    val rate = btcusd.rate
    Analysis(btcusd.symbol, btcusd.rate.formatRate, btcusd.change.format, 0.formatRate) ::
      candles.sortBy(-_.usdVolume)
        .take(candles.size / 3 * 2)
        .filter(_.rate < rate)
        .sortBy(_.rate).take(10)
        .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, (c.rate - rate).formatRate))
  }
}

case class Analysis(symbol: String, rate: String, change: String, rateCompareToBTC: String) {
  override def toString: String = {
    s"<https://www.bitfinex.com/t/${symbol.substring(1, 4)}:USD|$symbol>, $rate, $change, $rateCompareToBTC"
  }
}
