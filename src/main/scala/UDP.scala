package chat.udp
import chat.msg._
import akka.actor._
import java.util.Random
import scala.math
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.InetAddress

/**
 * Simple class for sending and receiving UDP packets
 * pt: Port number, nBytes: Size of packets to send
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
      //println("Listening")
      sender ! PACKET(sock.listen())
      //println("Got and sent")
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
      printer ! new MESSAGE(new String(data.getData))
      }
    
    //Got a request to send a packet somewhere
    case SEND(data,ip,port) => {
      //println("Sending")
      socket.send(data.getBytes,ip,port)
      //println("Done sending")
      }
    
    //Spray packets at a remote host to punch hole in local NAT
    case PUNCH(ip,port) => {
      for(i <- 1 to retransmit) {
        self ! SEND("HP_REQ",ip,port)
      }
    }//end case PUNCH
  }
}

//Print messages to the screen
class MsgPrinter extends Actor {
  def receive = {
    case MESSAGE(msg) =>
      println(msg)
  }
}

//object Main extends App{
//  
//  if(args.length != 3){
//    println("Wrong number of arguments")
//    exit(1)
//  }
//  val pt = args(1).toInt
//  val lh = InetAddress.getByName(args(0))
//  val name = args(2)
//  println(s"Connecting to $lh on port $pt")
//  val sock = new udp(pt)
//  val system = ActorSystem("ChatSystem")
//  val printer = system.actorOf(Props[MsgPrinter], name = "printer")
//  val sender = system.actorOf(Props(new UDPActor(sock,printer)), name = "sender")
//  sender ! PUNCH(lh)
//  while(true){
//    sender ! SEND(name+": "+readLine,lh,pt)
//  }
//}
