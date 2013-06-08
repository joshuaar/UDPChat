/**
 * Messages used by the network actors
 */
package chat.msg
import java.net.DatagramPacket
import java.net.InetAddress

abstract class msg
case class MESSAGE(msg:String) extends msg
case class LISTEN extends msg
case class PUNCH(ip:InetAddress)
case class SEND(data:String,ip:InetAddress,port:Int)
case class PACKET(data:DatagramPacket)
