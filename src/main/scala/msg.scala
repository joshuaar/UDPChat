/**
 * Messages used by the network actors
 */
package chat.msg
import java.net.DatagramPacket
import java.net.InetAddress
import java.io.File
abstract class msg
case class MESSAGE(msg:String) extends msg
case class RECVDMESSAGE(msg:String) extends msg
case class SENDMESSAGE(msg:String) extends msg
case class LISTEN(timeout:Int=0) extends msg
case class PUNCH(ip:InetAddress, port:Int)
case class PACKET(data:DatagramPacket)
case class CONNECT(remoteHost:String, remotePort:Int)
case class FILELISTEN(f:File)//Get file
case class FILERCVREADY // File ready to be received
case class SENDFILE(f:File)//Send file
case class SENDFILEREQ//Request send file
case class FILEDOWNLOADED(f:File)
case class INVALIDCOMMAND(e:String)
case class SEND(resource:String,host:String,port:Int)
case class GET(r:String,destination:String)

/**
 * Messages as part of RUDP
 */
abstract class RUDPMSG