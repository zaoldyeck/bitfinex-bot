import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object Helpers {

  implicit class JsValueExtension(jv: JsValue) {
    def toCandle(symbol: String): Candle = {
      val values = jv.as[List[JsValue]]
      val mts = values.head.as[Double]
      val open = values(1).as[Double]
      val close = values(2).as[Double]
      val high = values(3).as[Double]
      val low = values(4).as[Double]
      val volume = values.last.as[Double]
      Candle(symbol, mts, open, close, high, low, volume)
    }

    def toWallet: Wallet = {
      val values = jv.as[List[JsValue]]
      val walletType = values.head.as[String]
      val currency = values(1).as[String]
      val balance = values(2).as[Double]
      val unsettledInterest = values(3).as[Double]
      Wallet(walletType, currency, balance, unsettledInterest)
    }
  }

  implicit class DoubleExtension(d: Double) {
    def format: String = f"$d%8.4f"

    def formatRate: String = f"${d * 100}%6.2f" + "%"
  }

  def linearRegression(pairs: Seq[(Double, Double)]): (Double, Double, Double) = {
    val n = pairs.size

    val sumX = Future(pairs.map(_._1).sum)
    val sumY = Future(pairs.map(_._2).sum)
    val sumX2 = Future(pairs.map(x => Math.pow(x._1, 2)).sum)
    val sumY2 = Future(pairs.map(x => Math.pow(x._2, 2)).sum)
    val sumXY = Future(pairs.map(x => x._1 * x._2).sum)

    val dn = for {x2 <- sumX2; x <- sumX} yield n * x2 - x
    for (x <- dn) assert(x != 0.0, "Can't solve the system!")

    val future = for {
      x <- sumX
      y <- sumY
      x2 <- sumX2
      y2 <- sumY2
      xy <- sumXY
      d <- dn
      slope = (n * xy - x * y) / d
      intercept = (y * x2 - x * xy) / d
      t1 = ((n * xy) - (x * y)) * ((n * xy) - (x * y))
      t2 = (n * x2) - Math.pow(x, 2)
      t3 = (n * y2) - Math.pow(y, 2)
    } yield if (t2 * t3 != 0.0) (slope, intercept, t1 / (t2 * t3)) else (slope, intercept, 0.0)

    Await.result(future, Duration.Inf)
  }

  def linearRegressionV2(pairs: Seq[(Double, Double)]): (Double, Double, Double, Double) = {
    val size = pairs.size
    val xSeries = pairs.map(_._1)
    val ySeries = pairs.map(_._2)
    val averageX = xSeries.sum / size
    val averageY = ySeries.sum / size
    val diffX = xSeries.map(_ - averageX)
    val diffY = ySeries.map(_ - averageY)
    val powX = diffX.map(Math.pow(_, 2)).sum
    val powY = diffY.map(Math.pow(_, 2)).sum
    val XY = diffX.zip(diffY).map { case (x, y) => x * y }.sum
    val sdX = Math.sqrt(powX / size)
    val sdY = Math.sqrt(powY / size)
    val slope = XY / (Math.sqrt(powX) * Math.sqrt(powY)) * sdY / sdX
    val intercept = averageY - slope * averageX
    val ratesHigherPredict = (0 until size).map(slope * _ + intercept).zip(ySeries).map { case (predictY, realY) => realY - predictY }.filter(_ > 0)
    // y = ax +b
    val averageError = ratesHigherPredict.sum / ratesHigherPredict.size
    (slope, intercept, sdY, averageError)
  }
}
