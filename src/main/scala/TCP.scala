import chat.msg._
import chat.udp.udp
import net.rudp._
import java.net._
import akka.actor._
import java.io._

class rudpListener(s:ReliableSocket,parent:ActorRef) extends Actor{
  val in = new BufferedReader(new InputStreamReader(s.getInputStream()))
  while(true){
    println("Started Reading")
    //parent ! in.ready()
    parent ! in.readLine()
    println(in.readLine())
    println("Sent the result of the read")
  }
  def receive = {
    case _ => {
      }
  }
}
/**
 * Starts listening on a given port
 */
class rudpActor() extends Actor{
  import context._
  def connected:Receive = {
    case input:String => {
      println(input)
      }
  }
  def receive = {
    case LISTEN(pt) => {
      val serverSocket = new ReliableServerSocket(pt)
      self ! serverSocket.accept()
      }
    case s:ReliableSocket => {
      println("Connection Successful")
      val sock = actorOf(Props(new rudpListener(s,self)), name="listener")
      
      become(connected)
      }
  }
}

object rudp extends App {
  
  val system = ActorSystem("ChatSystem")
  val serv = system.actorOf(Props[rudpActor], name = "serv")
  serv ! LISTEN(6004)
  Thread.sleep(500)
  println("Attempting connection")
  val cli = new ReliableSocket("localhost",6004)
  println(cli.isConnected())
  println(cli.getLocalPort())
  val out = new PrintWriter(cli.getOutputStream(), true);
  while(true){
  out.println("Hello")
  }
}
