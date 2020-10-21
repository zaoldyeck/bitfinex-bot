import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.ws.ahc.{StandaloneAhcWSClient, StandaloneAhcWSRequest}

object Http {
  private implicit val system = ActorSystem()
  implicit val materializer = Materializer.matFromSystem
  system.registerOnTermination {
    System.exit(0)
  }

  val client = new StandaloneAhcWSClientWithProxyPool()

  def terminate(): Unit = {
    client.close()
    system.terminate()
  }

  class StandaloneAhcWSClientWithProxyPool {
    private val client = StandaloneAhcWSClient()

    def url(url: String, disableUrlEncoding: Boolean = false): StandaloneAhcWSRequest = {
      StandaloneAhcWSRequest(client = client,
        url = url,
        disableUrlEncoding = Some(disableUrlEncoding))
    }

    def close(): Unit = client.close()
  }

}
