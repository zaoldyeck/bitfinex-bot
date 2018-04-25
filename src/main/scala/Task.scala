import Extension._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class Task(implicit ec: ExecutionContext) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val bitfinexFetcher = new BitfinexFetcher

  def getAllSymbolCandles(symbols: List[Symbol], timeFrame: String = "5m"): Future[List[Candle]] = {
    Future.sequence(
      symbols
        .map(_.pair)
        .filter(_.endsWith("USD"))
        .map(s => bitfinexFetcher.getLastCandle(s, timeFrame)))
  }

  def filterStrongSymbols(candles: List[Candle]): List[Analysis] = {
    val btcusd = candles.filter(_.symbol == "tBTCUSD").head
    val rate = btcusd.rate
    Analysis(btcusd.symbol, btcusd.rate.formatRate, btcusd.change.format, 0.formatRate) ::
      candles.sortBy(-_.usdVolume)
        .take(candles.size / 2)
        .filter(_.rate > rate)
        .sortBy(-_.rate)
        .take(10)
        .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, (c.rate - rate).formatRate))
  }

  def filterWeakSymbols(symbols: List[Symbol], candles: List[Candle]): List[Analysis] = {
    val pairToMargin = symbols.map(symbol => symbol.pair -> symbol.margin).toMap
    val btcusd = candles.filter(_.symbol == "tBTCUSD").head
    val rate = btcusd.rate
    Analysis(btcusd.symbol, btcusd.rate.formatRate, btcusd.change.format, 0.formatRate) ::
      candles
        .filter(candle => pairToMargin(candle.symbol) && candle.rate < rate)
        .sortBy(_.rate)
        .take(10)
        .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, (c.rate - rate).formatRate))
  }

  def filterRisingSymbols(candles: List[Candle]): List[Analysis] = {
    candles.sortBy(-_.usdVolume)
      .take(candles.size / 2)
      .filter(_.rate > 0.01)
      .sortBy(-_.rate)
      .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, ""))
  }

  def filterFallingSymbols(symbols: List[Symbol], candles: List[Candle]): List[Analysis] = {
    val pairToMargin = symbols.map(symbol => symbol.pair -> symbol.margin).toMap
    candles
      .filter(candle => candle.rate < -0.01 && pairToMargin(candle.symbol))
      .sortBy(_.rate)
      .map(c => Analysis(c.symbol, c.rate.formatRate, c.change.format, ""))
  }
}

case class Analysis(_symbol: String, rate: String, change: String, rateCompareToBTC: String) {
  val symbol: String = _symbol.drop(1)

  override def toString: String = {
    s"<https://www.bitfinex.com/t/${symbol.dropRight(3)}:USD|$symbol>, $rate, $change, $rateCompareToBTC"
  }
}
