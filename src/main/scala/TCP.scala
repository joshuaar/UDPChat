import chat.msg._
import chat.udp.udp
import net.rudp._
import java.net._
import akka.actor._

/**
 * Starts listening on a given port
 */
class rudpActor() extends Actor{
  import context._
  var sock:ReliableSocket
  def connected: Receive = {
    case 
}
  def receive = {
    case LISTEN(pt) => {
      val serverSocket = new ReliableServerSocket(pt)
      self ! serverSocket.accept()
      }
    case s:ReliableSocket => {
      println("Connection Successful")
      sock = s
      become(connected)
      }
  }
}

object rudp extends App {
  
  val system = ActorSystem("ChatSystem")
  val serv = system.actorOf(Props[rudpActor], name = "serv")
  serv ! LISTEN(6004)
  val cli = new ReliableSocket("localhost",6004)
  println(cli.isConnected())
}
