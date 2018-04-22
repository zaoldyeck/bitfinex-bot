import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.{StandaloneAhcWSClient, StandaloneAhcWSRequest}

object Http {
  implicit private val system = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }

  val client = new StandaloneAhcWSClientWithProxyPool()

  def terminate(): Unit = {
    client.close()
    system.terminate()
  }

  class StandaloneAhcWSClientWithProxyPool {
    private implicit val materializer = ActorMaterializer()
    private val client = StandaloneAhcWSClient()

    def url(url: String, disableUrlEncoding: Boolean = false): StandaloneAhcWSRequest = {
      StandaloneAhcWSRequest(client = client,
        url = url,
        disableUrlEncoding = Some(disableUrlEncoding))
    }

    def close(): Unit = client.close()
  }

}
