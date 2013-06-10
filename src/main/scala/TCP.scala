import chat.msg._
import chat.udp.udp
import net.rudp._
import java.net._
import akka.actor._
import java.io._

object wireCodes {
  val FT_MODE = "$f"//Send this to request client gets ready for a file
  val FT_RDY = "$r" //Send this to host when ready for file
  val CMD_MODE = "$c"
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
    case SENDFILEREQ => {
      out.println(wireCodes.FT_MODE)
    }
    case SENDFILE(f) =>
      become(fileSend)
      out.close()
      self ! SENDFILE(f)
  }
  def fileSend: Receive = {
    case SENDFILE(f) => {
      val buffer = new Array[Byte](2048)
      val fileIn = new FileInputStream(f)
      val fileOut = new BufferedOutputStream(s.getOutputStream())
      var len = fileIn.read(buffer)
      while(len != -1) {
        fileOut.write(buffer, 0,len)
        len = fileIn.read(buffer)
      }
      val fileName = f.getName()
      val host = s.getInetAddress().getHostName()
      println(s"File $fileName sent successfully to $host")
      out = new PrintWriter(s.getOutputStream(), true);
      unbecome()
    }
    case _ => {
      sender ! INVALIDCOMMAND("Cannot send commands during file transfer")
    }
  }
}

//Listens for messages and files from remote
class rudpListener(s:ReliableSocket,parent:ActorRef) extends Actor{
  import context._
  var in = new BufferedReader(new InputStreamReader(s.getInputStream()))
  self ! LISTEN
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
      println("Listener Online")
      val data = in.readLine()
      println("Read something")
      data match {
        
        case wireCodes.FT_MODE => { //Must receive file transfer mode request from remote
          in.close() //Close the current line reader
          become(fileListen) //Go into file listening mode
          parent ! wireCodes.FT_RDY //Notify Parent of mode change
        }
        
        case s:String => {
          println("I read a string")
          parent ! RECVDMESSAGE(s) //Send command to parent if it is unknown
          println("Sent the string to the UDP actor")
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

class SenderListenerPair(initsender:ActorRef,initlistener:ActorRef) {
  val sender = initsender
  val listener = initlistener
}
/**
 * Starts listening on a given port
 */
class rudpActor(lp:Int) extends Actor{
  val localPort = lp
  val localHost = InetAddress.getLocalHost()
  var senderListener = null.asInstanceOf[SenderListenerPair]
  
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
      val listen = actorOf(Props(new rudpListener(s,self)), name="listen")
      val send = actorOf(Props(new rudpSender(s,self)), name="send")
      senderListener = new SenderListenerPair(send,listen)
      become(connected)
      println(s"connected to $hostName on remote port $port")

      }
  }
  
  def connected:Receive = {
    case RECVDMESSAGE(s) => {
      println("I got a message on the wire!: "+s)
    }
    case wireCodes.FT_RDY => {
      //Send the remote a signal that files are ready to be sent
    }
    case m:SENDMESSAGE => {
      println("outer sending message")
      senderListener.sender ! m
      println("outer sent")
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
  while(true){
    cli ! SENDMESSAGE(readLine)
    serv ! SENDMESSAGE(readLine)
  }
  
  
}
