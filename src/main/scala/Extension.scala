import play.api.libs.json.JsValue

object Extension {

  implicit class RichJsValue(jv: JsValue) {
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
  }

  implicit class RichDouble(d: Double) {
    def format: String = f"$d%8.4f"

    def formatRate: String = f"${d * 100}%6.2f" + "%"
  }

}
