import chat.msg._
import net.rudp._
import java.net._
import akka.actor._
import java.io._
import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._


object wireCodes {
  val FT_CON = ":c(.*):(.*)".r
  val FT_REQ = "::(.*):(.*):(.*)".r//Send this plus to request a resource from server
  val FT_RDY = ":RDY" //Send this to host when ready for file
  val FT_STOP = ":STP"
  def sendreq(resource:String,ip:String,port:Int):String ={
    return s"::$resource:$ip:$port"
  }
  def getLocalPort():Int = {
    val s= new ReliableServerSocket(0)
    val port = s.getLocalPort()
    s.close()
    return port
  }
  def copy(in:InputStream, out:OutputStream) {
        val buf = new Array[Byte](2048)
        var len = 0;
        len = in.read(buf)
        while (len != -1) {
            out.write(buf, 0, len);
            println(s"Sent $len bytes")
            len = in.read(buf)
        }
    }
}

//Sends messages and files to remote
class rudpSender(s:ReliableSocket,parent:ActorRef) extends Actor{
  import context._
  var out = new PrintWriter(s.getOutputStream(), true);
  def receive = {
    case SENDMESSAGE(m) => {
      println("inner send message")
      out.println(m)
      println("inner sent")
    }
  }
}

//Listens for messages and files from remote
class rudpListener(s:ReliableSocket,parent:ActorRef) extends Actor{
  import context._
  var in = new BufferedReader(new InputStreamReader(s.getInputStream()))
  def receive = {
    case LISTEN => {
      become(cmdListen)
      self ! LISTEN
    }
  }
  def cmdListen:Receive = {
    //Listens for lines of strings
    case LISTEN => {
      println("Listener Online")
      val data = in.readLine()
      data match {
        case wireCodes.FT_REQ(id,host,port) => {
          parent ! SEND(id,host,port.toInt) //Initiate request
          println(s"Recieved a get request from $host, sending to handler")
        }
        case wireCodes.FT_STOP => {
          println("stopped listening on request from remote")
        }
        case wireCodes.FT_CON(ip,port) => {
          println("Now I got the connection information from remote")
          parent ! (ip,port)
        }
        case s:String => {
          parent ! RECVDMESSAGE(s)
          println("Sent unknown message to handler")
        }
      }
    }
  }//End cmdListener
}

class fileListener(lp:Int,destination:String) extends Actor{
  import context._
  
  //ServerSocket attempt a listen
  val serverSocket = new ReliableServerSocket(lp)
  val future = Future{ serverSocket.accept() }
  val sock:ReliableSocket = null
  println("File getter beginning connection")
  //Failure to connect, or connect
  try{ self ! Await.result(future, 60 second).asInstanceOf[ReliableSocket] }
  catch{ 
    case t:TimeoutException => {
      println("File listen timeout")
      self ! PoisonPill // kill
      }
    }
  
  def receive = {
    case s:ReliableSocket => {
      println("File getter is connected")
      val buffer = new Array[Byte](2048)
      val f = new File(destination)
      val fileOut = new FileOutputStream(f)
      val fileIn = s.getInputStream()
      println("Started file receiving")
      var len = fileIn.read(buffer)
      while(len != -1) {
        println(s"Got $len bytes")
        fileOut.write(buffer,0,len)
        len = fileIn.read(buffer)
      }
      s.close()
      fileOut.close()
      println("Finished file reception")
      self ! PoisonPill
    }
    case _ => println("Received a message")
  }
}

class fileSender(filePath:String,host:String,port:Int,localHost:InetAddress,localPort:Int) extends Actor{
  val sock = new ReliableSocket(host,port,localHost,localPort)
  println("file sender is connected")
  self ! sock
  def receive = {
    case s:ReliableSocket => {
      val buffer = new Array[Byte](2048)
      val f = new File(filePath)
      val fileIn = new FileInputStream(f)
      val fileOut = s.getOutputStream()
      wireCodes.copy(fileIn,fileOut)
      fileOut.close()
      fileIn.close() // close file
      s.close()
      //Now file has been sent
      println("Finished file sending")
      self ! PoisonPill
    }
  }
}

/**
* Starts listening on a given port
*/
class rudpActor(lp:Int) extends Actor{
  val localPort = lp
  val localHost = InetAddress.getLocalHost()
  var listener:ActorRef = null
  var send:PrintWriter = null
  
  import context._
  def receive = {
    
    case LISTEN => {
      println(s"Listening on $localPort")
      val serverSocket = new ReliableServerSocket(localPort)
      self ! serverSocket.accept()
      }
    
    case CONNECT(ip,port) => {
      println(s"Attempting RUDP Connection to $ip on port $port")
      val cli = new ReliableSocket(ip,port,localHost,localPort)
      self ! cli
    }
    
    case s:ReliableSocket => {
      val hostName = s.getInetAddress().getCanonicalHostName()
      val port = s.getPort()
      listener = context.actorOf(Props(new rudpListener(s,self)), name="listen")
      listener ! LISTEN
      send = new PrintWriter(s.getOutputStream(), true)
      become(connected)
      println(s"connected to $hostName on remote port $port")

      }
  }
  
  def connected:Receive = {
    case RECVDMESSAGE(s) => {
      println("I got a message on the wire!: "+s)
      listener ! LISTEN
    }
    case SENDMESSAGE(m) => {
      println("outer sending message")
      send.println(m)
      println("outer sent")
    }
    case GET(resource,destination) => {
      
      //Determine from STUN server which IP and port to connect on
      val localPort = wireCodes.getLocalPort()
      val lh = localHost.getHostName() 
      
      //Initialize listener
      val fileGetter = context.actorOf(Props(new fileListener(localPort,destination)))
      
      send.println(wireCodes.sendreq(resource,lh,localPort)) // send a request for a resource

    }
    
    case SEND(resource,host,port) => { //got a request from remote for resource
      println("Got request from remote for a resource")
      val fileSender = context.actorOf(Props(new fileSender(resource,host,port,localHost,6009)))
    }
  }
}

object rudp extends App {
  
  val system = ActorSystem("ChatSystem")
  val serv = system.actorOf(Props(new rudpActor(6004)), name = "serv")
  val cli = system.actorOf(Props(new rudpActor(6005)), name = "cli")
  serv ! LISTEN
  Thread.sleep(500)
  println("Attempting connection")
  cli ! CONNECT("localhost",6004)
  println("Testing Message sending")
  Thread.sleep(1000)
  
  //Test send message
  cli ! SENDMESSAGE("Message1")
  serv ! SENDMESSAGE("Message2")
  
  //Test file request
  cli ! GET("/home/josh/UDPChat/jars/UDPChat.jar","/home/josh/test2")//GET(remoteLocation,localLocation)
  
}