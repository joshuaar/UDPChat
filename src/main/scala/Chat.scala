import akka.actor._
import java.util.Random
import scala.math
import java.net.DatagramPacket
import java.net.DatagramSocket


abstract class msg
case class MESSAGE(from:String,msg:String) extends msg
case class SUCCESS extends msg
case class STOP extends msg

object udp { 
  def echo() = {
    println("starting")
  val bufsize = 16 
  val port = 4444
  val sock = new DatagramSocket(port)
  val buf = new Array[Byte](bufsize)
  val packet = new DatagramPacket(buf, bufsize)
  while (true) {
    println("looping")
    sock.receive(packet)
    println("received packet from: " + packet.getAddress())
    sock.send(packet)
    println("echoed data (first 16 bytess): " +
            packet.getData().take(16).toString())
  }
}

}

class MsgPrinter extends Actor {
  def receive = {
    case MESSAGE(from,msg) =>
      println(from+": "+msg)
  }
}

class MsgSender(printer:ActorRef) extends Actor {
  while(true){
	  Thread.sleep(1000)
	  printer ! MESSAGE("Gremlin","Im an actor")
  }
  def receive = {
    case STOP => 
      self ! PoisonPill
  }
}

object Main extends App{
  udp.echo()
  println("Starting")
  val system = ActorSystem("ChatSystem")
  val printer = system.actorOf(Props[MsgPrinter], name = "printer")
  val sender = system.actorOf(Props(new MsgSender(printer)), name = "sender")
  println("Looping")
  // start input loop
  while(true){
    printer ! MESSAGE("Josh",readLine)
  }
}
