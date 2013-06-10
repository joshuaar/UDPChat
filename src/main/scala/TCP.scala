import chat.msg._
import chat.udp.udp
import net.rudp._
import java.net._
import akka.actor._
import java.io._

object wireCodes {
  val FT_MODE = "$f"
  val FT_RDY = "$r"
  val CMD_MODE = "$c"
}

class rudpSender(s:ReliableSocket,parent:ActorRef) extends Actor{
  var out = new PrintWriter(s.getOutputStream(), true);
  //Write the sending code here
}

class rudpListener(s:ReliableSocket,parent:ActorRef) extends Actor{
  import context._
  var in = new BufferedReader(new InputStreamReader(s.getInputStream()))
  
  def receive = {
    case LISTEN => {
      become(cmdListen)
      self ! LISTEN
    }
    case FILELISTEN(file) =>
      in.close()
      become(fileListen)
  }
  
  def cmdListen:Receive = {
    //Listens for lines of strings
    case LISTEN => {
      val data = in.readLine()
      println(data)
      
      data match {
        
        case wireCodes.FT_MODE => { //Must receive file transfer mode request from remote
          in.close() //Close the current line reader
          become(fileListen) //Go into file listening mode
          parent ! wireCodes.FT_RDY //Notify Parent of mode change
        }
        
        case s:String => {
          parent ! REMOTEMESSAGE(s) //Send command to parent if it is unknown
          self ! LISTEN //Repeat listening
        }
        
      }//End match
    }
  }
  
  def fileListen:Receive = {
    
    case LISTEN => {
      println("Invalid, Cannot Listen for Wire Commands during file transfer")
      //sender ! INVALIDCOMMAND("Cannot listen for commands during file transfer")
    }
    
    case FILELISTEN(file) => {//Listen for files
      val fileIn = new BufferedInputStream(s.getInputStream())
      val fileDest = new FileOutputStream(file)
      val buffer = new Array[Byte](2048)//Adjust if desired
      
      var counter = 0
      var bytesRead = 0
      while(bytesRead >=0 ) {
        bytesRead = fileIn.read(buffer, 0, bytesRead)
        if(bytesRead >= 0) {
          fileDest.write(buffer)
          counter += bytesRead
        }
      }
      
      val path = file.getAbsolutePath()
      println(s"Downloaded File of size $counter to $path")
      fileDest.close()
      
      in = new BufferedReader(new InputStreamReader(s.getInputStream()))
      become(cmdListen)//Go back to command listen mode
      sender ! FILEDOWNLOADED(file) //Notify the sender that the file has been downloaded
    }
  }
}

/**
 * Starts listening on a given port
 */
class rudpActor(lp:Int) extends Actor{
  val localPort = lp
  val localHost = InetAddress.getLocalHost()
  import context._

  def receive = {
    
    case LISTEN => {
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
      println(s"connected to $hostName on remote port $port")
      val sock = actorOf(Props(new rudpListener(s,self)), name="sock")
      become(connected)
      }
  }
  def connected:Receive = {
    case REMOTEMESSAGE(s) => {
      println(s)
    }
    case wireCodes.FT_RDY => {
      //Send the remote a signal that files are ready to be sent
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
  
  
  
}
