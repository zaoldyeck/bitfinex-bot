import Extension._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class Task(implicit ec: ExecutionContext) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val bitfinexFetcher = new BitfinexFetcher

  def getAllSymbolCandles(symbols: List[Symbol]): Future[List[Candle]] = {
    Future.sequence(
      symbols
        .map(_.pair)
        .filter(_.endsWith("USD"))
        .map(s => bitfinexFetcher.getLastCandle(s)))
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

  def filterWeakSymbols(symbols: List[Symbol], candles: List[Candle]): List[Analysis] = {
    val pairToMargin = symbols.map(symbol => symbol.pair -> symbol.margin).toMap
    val btcusd = candles.filter(_.symbol == "tBTCUSD").head
    val rate = btcusd.rate
    Analysis(btcusd.symbol, btcusd.rate.formatRate, btcusd.change.format, 0.formatRate) ::
      candles
        .filter(candle => pairToMargin(candle.symbol) && candle.rate < rate)
        .sortBy(_.rate).take(10)
        .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, (c.rate - rate).formatRate))
  }
}

case class Analysis(symbol: String, rate: String, change: String, rateCompareToBTC: String) {
  override def toString: String = {
    s"<https://www.bitfinex.com/t/${symbol.substring(1, 4)}:USD|$symbol>, $rate, $change, $rateCompareToBTC"
  }
}
