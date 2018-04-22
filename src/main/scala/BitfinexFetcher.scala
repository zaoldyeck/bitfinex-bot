import Extension._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyReadables._

import scala.concurrent.{ExecutionContext, Future}

class BitfinexFetcher(implicit ec: ExecutionContext) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  implicit val candleReads = Json.reads[Candle]

  def getAllSymbols: Future[List[String]] = {
    Http.client.url("https://api.bitfinex.com/v1/symbols").get.map {
      response =>
        response.body[JsValue].as[List[String]]
    }
  }

  def getLastCandle(symbol: String, timeFrame: String = "1h"): Future[Candle] = {
    Http.client.url(s"https://api.bitfinex.com/v2/candles/trade:$timeFrame:$symbol/last").get.map {
      response =>
        response.body[JsValue].toCandle(symbol)
    }
  } recover {
    case e: Exception =>
      e.printStackTrace()
      logger.error(symbol)
      Candle(symbol, 0, 0, 0, 0, 0, 0)
  }

  def getHistCandle(symbol: String, timeFrame: String = "1h"): Future[List[Candle]] = {
    Http.client.url(s"https://api.bitfinex.com/v2/candles/trade:$timeFrame:$symbol/hist").get.map {
      response =>
        response.body[JsValue].as[List[JsValue]].map(_.toCandle(symbol))
    }
  } recover {
    case e: Exception =>
      e.printStackTrace()
      logger.error(symbol)
      List()
  }
}

case class Candle(symbol: String, mts: Double, open: Double, close: Double, high: Double, low: Double, volume: Double) {
  val change: Double = close - open
  val rate: Double = change / open
  val usdVolume: Double = close * volume
}
