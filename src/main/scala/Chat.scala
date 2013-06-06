import akka.actor._
import java.util.Random
import scala.math


abstract class msg
case class MESSAGE(from:String,msg:String) extends msg
case class SUCCESS extends msg
case class STOP extends msg

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
  val system = ActorSystem("ChatSystem")
  val printer = system.actorOf(Props[MsgPrinter], name = "printer")
  val sender = system.actorOf(Props(new MsgSender(printer)), name = "sender")
  println("Looping")
  // start input loop
  while(true){
    printer ! MESSAGE("Josh",readLine)
  }
}
