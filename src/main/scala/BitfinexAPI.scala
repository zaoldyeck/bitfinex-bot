
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

import Helpers._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json, Reads}
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.{StandaloneWSRequest, StandaloneWSResponse}
import play.api.libs.ws.ahc.AhcCurlRequestLogger

import scala.concurrent.{ExecutionContext, Future}

class BitfinexAPI(implicit ec: ExecutionContext) {
  private val conf: Config = ConfigFactory.load()
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private implicit val candleReads: Reads[Candle] = Json.reads[Candle]

  def getAllSymbols: Future[Seq[Symbol]] = {
    val endpoint = conf.getString("bitfinex.api.symbols")
    Http.client.url(endpoint).get.map {
      response =>
        response.body[JsValue].as[Seq[JsValue]].map {
          json =>
            val pair: String = "t" + (json \ "pair").as[String].toUpperCase
            val margin = (json \ "margin").as[Boolean]
            Symbol(pair, margin)
        }
    }
  }

  def getLastCandle(symbol: String, timeFrame: String = "5m"): Future[Candle] = {
    val endpoint = conf.getString("bitfinex.api.candles")
    Http.client.url(s"$endpoint$timeFrame:$symbol/last").get.map {
      response =>
        response.body[JsValue].toCandle(symbol)
    }
  } recover {
    case e: Exception =>
      e.printStackTrace()
      logger.error(symbol)
      Candle(symbol, 0, 0, 0, 0, 0, 0)
  }

  def getHistCandle(symbol: String, timeFrame: String = "5m", period: String = "", limit: Int = 1000): Future[Seq[Candle]] = {
    val endpoint = conf.getString("bitfinex.api.candles")
    Http.client.url(s"$endpoint$timeFrame:$symbol$period/hist?limit=$limit").get.map {
      response =>
        response.body[JsValue].as[Seq[JsValue]].map(_.toCandle(symbol))
    }
  } recover {
    case e: Exception =>
      e.printStackTrace()
      logger.error(symbol)
      Seq.empty
  }

  def getWallets: Future[Seq[Wallet]] = {
    val apiPath = "/api/v2/auth/r/wallets"
    val body = JsObject.empty
    val endpoint = conf.getString("bitfinex.api.wallets")
    val headers = getHeaders(apiPath, body)
    Http.client.url(endpoint)
      .withHttpHeaders(headers: _*)
      .post(body)
      .map {
        response =>
          response.body[JsValue].as[Seq[JsValue]].map(_.toWallet)
      }
  }

  def getHighestRateOfActiveFundingOffers(symbol: String = "fUSD"): Future[Double] = {
    val apiPath = s"/api/v2/auth/r/funding/offers/$symbol"
    val body = JsObject.empty
    val endpoint = conf.getString("bitfinex.api.activeFundingOffers") + symbol
    val headers = getHeaders(apiPath, body)
    Http.client.url(endpoint)
      .withHttpHeaders(headers: _*)
      .post(body)
      .map {
        response =>
          val rates = response.body[JsValue].as[Seq[JsArray]].map(values => values(14).as[Double])
          if (rates.isEmpty) 0.0 else rates.max
      }
  }

  def submitFundingOffer(amount: Double, rate: Double, `type`: String = "LIMIT", symbol: String = "fUSD", period: Int = 2): Future[String] = {
    val apiPath = "/api/v2/auth/w/funding/offer/submit"
    val body = Json.obj("type" -> `type`,
      "symbol" -> symbol,
      "amount" -> amount.toString,
      "rate" -> rate.toString,
      "period" -> period)
    val headers = getHeaders(apiPath, body)

    val endpoint = conf.getString("bitfinex.api.submitFundingOffer")
    Http.client.url(endpoint)
      .withHttpHeaders(headers: _*)
      .post(body)
      .map {
        response =>
          response.body[JsValue].as[Seq[JsValue]].apply(7).as[String]
      }
  }

  def cancelAllFundingOffers(currency: String = "USD"): Future[StandaloneWSRequest#Response] = {
    val apiPath = "/api/v2/auth/w/funding/offer/cancel/all"
    val body = Json.obj("currency" -> currency)
    val headers = getHeaders(apiPath, body)

    val endpoint = conf.getString("bitfinex.api.cancelAllFundingOffers")
    Http.client.url(endpoint)
      .withHttpHeaders(headers: _*)
      .post(body)
  }

  def getSumOfFundingCredits(symbol: String = "fUSD"): Future[Double] = {
    val apiPath = s"/api/v2/auth/r/funding/credits/$symbol"
    val body = JsObject.empty
    val endpoint = conf.getString("bitfinex.api.fundingCredits") + symbol
    val headers = getHeaders(apiPath, body)
    Http.client.url(endpoint)
      .withHttpHeaders(headers: _*)
      .post(body)
      .map {
        response =>
          response.body[JsValue].as[Seq[JsArray]].map(values => values(5).as[Double]).sum
      }
  }

  private def getHeaders(apiPath: String, body: JsObject): Seq[(String, String)] = {
    val key = conf.getString("bitfinex.key")
    val secret = conf.getString("bitfinex.secret")
    val nonce = Instant.now.getEpochSecond.toString
    val signature = s"$apiPath$nonce${body.toString}"
    val sig = new HmacUtils(HmacAlgorithms.HMAC_SHA_384, secret).hmacHex(signature)
    Seq("bfx-nonce" -> nonce,
      "bfx-apikey" -> key,
      "bfx-signature" -> sig)
  }
}

case class Symbol(pair: String, margin: Boolean)

case class Candle(symbol: String, mts: Double, open: Double, close: Double, high: Double, low: Double, volume: Double) {
  val change: Double = close - open
  val rate: Double = change / open
  val usdVolume: Double = close * volume
}

case class Wallet(walletType: String, currency: String, balance: Double, unsettledInterest: Double)