/**
 * Messages used by the network actors
 */
package chat.msg
import java.net.DatagramPacket
import java.net.InetAddress

abstract class msg
case class MESSAGE(msg:String) extends msg
case class LISTEN(port:Int=0) extends msg
case class PUNCH(ip:InetAddress)
case class SEND(data:String,ip:InetAddress,port:Int)
case class PACKET(data:DatagramPacket)
case class CONNECT(remoteHost:String, remotePort:Int)

/**
 * Messages as part of RUDP
 */
abstract class RUDPMSG