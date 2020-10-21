import org.scalatest.funsuite.AsyncFunSuite
import Helpers._

import scala.collection.mutable

class BitfinexFetcherTest extends AsyncFunSuite {
  private val bitfinexAPI = new BitfinexAPI

  test("getAllSymbols") {
    bitfinexAPI.getAllSymbols.map {
      symbols =>
        symbols.foreach(println)
        println(symbols.size)
        assert(symbols.nonEmpty)
    }
  }

  test("testGetLastCandle") {
    bitfinexAPI.getLastCandle("tBTCUSD").map {
      candle =>
        println(candle)
        assert(candle.close != 0)
    }
  }


  test("getHistCandle") {
    bitfinexAPI.getHistCandle("tBTCUSD").map {
      candles =>
        println(candles)
        assert(candles.nonEmpty)
    }
  }

  test("getFundingHistCandle") {
    bitfinexAPI.getHistCandle("fUSD", "1m", ":p2", 120).map {
      candles =>
        val heightRates = candles.map(_.high)
        val max = BigDecimal(heightRates.max)
        val average = BigDecimal(heightRates.sum / heightRates.size)
        val min = BigDecimal(heightRates.min)

        val diffROI = max * 2 - average * 2
        val allowMinutes = diffROI / (average / 24 / 60)

        val timeToPrice = heightRates.zipWithIndex.map { case (value, index) => (index.toDouble, value) }
        val result = linearRegressionV2(timeToPrice)
        val predictRate = BigDecimal(120 * result._1 + result._2)

        println(max)
        println(average)
        println(min)
        println(diffROI)
        println(allowMinutes)

        println(BigDecimal(result._1))
        println(BigDecimal(result._2))
        println(BigDecimal(result._3))
        println(BigDecimal(result._4))
        println(s"predictRate: $predictRate")
        println(predictRate + result._3)
        println(predictRate + result._4)

        val ratesHigherAverage = heightRates.map(_ - average).filter(_ > 0)
        val averageHigherRate = ratesHigherAverage.sum / ratesHigherAverage.size
        println(s"averageHigherRate: $averageHigherRate")
        println(average + averageHigherRate)
        println(predictRate + averageHigherRate)

        val maxHigherRate = ratesHigherAverage.max
        println(s"maxHigherRate: $maxHigherRate")
        println(average + maxHigherRate)
        println(predictRate + maxHigherRate)

        var maxR = heightRates.head
        var maxDiff = 0.0
        val diffs = mutable.ArrayBuffer.empty[Double]
        heightRates.foreach {
          r =>
            if (r > maxR) {
              val diff = r - maxR
              diffs += diff
              //if (diff > maxDiff) maxDiff = diff
              maxDiff = diff
              maxR = r
            }
        }
        val averageDiff = diffs.sum / diffs.size

        println(BigDecimal(maxR))
        println(s"maxDiff: ${BigDecimal(maxDiff)}")
        println(s"averageDiff: ${BigDecimal(averageDiff)}")
        println(average + maxDiff)
        println(predictRate + maxDiff)
        println(max + maxDiff)
        println(max + maxDiff / 2)
        println(max + averageDiff)

        assert(candles.nonEmpty)
    }
  }
}
