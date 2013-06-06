package chat.udp
import akka.actor._
import java.util.Random
import scala.math
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.InetAddress

abstract class msg
case class MESSAGE(from:String,msg:String) extends msg
case class SUCCESS extends msg
case class STOP extends msg
case class LISTEN extends msg
case class PUNCH(ip:InetAddress)
case class SEND(data:String,ip:InetAddress,port:Int)
case class PACKET(data:DatagramPacket)

/**
 * Simple class for sending and receiving UDP packets
 */
class udp(pt:Int, nBytes:Int=2048) { 
  val port = pt
  val sock = new DatagramSocket(pt)  
  /**
   * Listens for a single packet on a socket
   */
  def listen():DatagramPacket={
    val packet = new Array[Byte](nBytes)
    val receivePacket = new DatagramPacket(packet,packet.length)
    sock.receive(receivePacket)
    return receivePacket
  }
  /**
   * Sends a packet to a remote host
   */
  def send(data:Array[Byte],IP:InetAddress,port:Int){
    val toSend = new DatagramPacket(data,data.length,IP,port)
    sock.send(toSend)
  }
}

/**
 * An actor that listens on a UDP socket.
 * args: 
 * 	socket: A udp socket object
 */
class UDPListener(socket:udp) extends Actor {
  val sock = socket
  def receive = {
    case LISTEN => {
      println("Listening")
      sender ! PACKET(sock.listen())
      println("Got and sent")
      }
  }
}

class UDPActor(socket:udp,printer:ActorRef) extends Actor {
  
  //Number of retransmissions during punching
  val retransmit = 60
  
  import context._
  //Start up the listener
  val listener = actorOf(Props(new UDPListener(socket)), name="listener")
  listener ! LISTEN
  
  def receive = {
    //Got a packet from the listener
    case PACKET(data) => {
      listener ! LISTEN
      printer ! new MESSAGE("default",new String(data.getData))
      }
    
    //Got a request to send a packet somewhere
    case SEND(data,ip,port) => {
      println("Sending")
      socket.send(data.getBytes,ip,port)
      println("Done sending")
      }
    
    //Spray packets at a remote host to punch hole in local NAT
    case PUNCH(ip) => {
      for(i <- 1 to retransmit) {
        self ! SEND("HP_REQ",ip,socket.port)
      }
    }//end case PUNCH
  }
}

//Print messages to the screen
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
  println("Starting")
  val pt = 6005
  val lh = InetAddress.getByName("localhost")
  val sock = new udp(pt)
  val system = ActorSystem("ChatSystem")
  val printer = system.actorOf(Props[MsgPrinter], name = "printer")
  val sender = system.actorOf(Props(new UDPActor(sock,printer)), name = "sender")
  println("Looping")
  sender ! PUNCH(lh)
  while(true){
    sender ! SEND(readLine,lh,pt)
  }
  

}
