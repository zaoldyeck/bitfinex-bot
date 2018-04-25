import org.scalatest.AsyncFunSuite

class BitfinexFetcherTest extends AsyncFunSuite {
  private val bitfinexFetcher = new BitfinexFetcher

  test("getAllSymbols") {
    bitfinexFetcher.getAllSymbols.map {
      symbols =>
        symbols.foreach(println)
        println(symbols.size)
        assert(symbols.nonEmpty)
    }
  }

  test("testGetLastCandle") {
    bitfinexFetcher.getLastCandle("tBTCUSD").map {
      candle =>
        println(candle)
        assert(candle.close != 0)
    }
  }

  test("getHistCandle") {
    bitfinexFetcher.getHistCandle("tBTCUSD").map {
      candles =>
        println(candles)
        assert(candles.nonEmpty)
    }
  }
}
