UDPChat
=======

command line P2P chat over UDP

## Description
This is a simple chat client over UDP. It uses the principal known as UDP hole punching to get past NATs.

##Usage
1. Find the jar file in /jar
2. Get your friend to download the same file
3. Exchange IP address information with the friend (type my ip into google)
4. Pick a port (e.g 6004)
5. Execute the jar as follows:
	java -jar UDPChat.jar RemoteIP Port Name
where RemoteIP is your friends IP address, Port is the port you agreed upon, and Name is the nickname you would like to appear in the chat.

##Details
This uses UDP hole punching to get through any NATs you and your friend may be behind. Normally you would discover
eachother's IP addresses through some external server, but in this simple example we just exchange that information manually.
