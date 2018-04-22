import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSResponse

import scala.concurrent.Future

class SlackBot {
  private val conf: Config = ConfigFactory.load()
  private val url = conf.getString("slack.webhooks.url")

  def sendMessage(message: String): Future[StandaloneWSResponse] = {
    val data = Json.obj(
      "username" -> "bitfinex-bot",
      "text" -> message
    )
    Http.client.url(url).post(data)
  }
}
