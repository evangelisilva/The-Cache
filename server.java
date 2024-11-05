import java.io.*;
import java.net.*;

/**
 * The Server class handles incoming client requests to upload (put) or download (get) files.
 * It supports both TCP and Stop-and-Wait (SNW) protocols for data transfer.
 * 
 * Usage:
 *     java server <port> <protocol>
 * 
 * Parameters:
 * - <port>: Port number to start the server.
 * - <protocol>: Transport protocol ("tcp" or "snw") to be used for file transfers.
 */
public class server {
	
	// Protocol handlers for TCP and Stop-and-Wait (SNW) transports
    static tcp_transport tcpTransport = new tcp_transport();
    static snw_transport snwTransport = new snw_transport(); 

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java server <port> <protocol>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String protocol = args[1].toLowerCase();
        
        if (!protocol.equals("tcp") &&  !protocol.equals("snw")) {
        	System.out.println("Error: Invalid protocol");
        	return;
        }
        
        File directory = new File("server_fl");
        if (!directory.exists()) 
        	directory.mkdirs();
        
        
        // Start the server socket on the specified port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(protocol + " server started on port " + port);

            while (true) {
            	// Accept client connections
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
                    
                    // Read and process the client's command
                    String command = dis.readUTF();
                    if (command.startsWith("put")) {
                    	// Handle file upload from client
                    	if (protocol.equals("tcp")) {
                    		tcpTransport.receiveFile(dis, dos);
                    	} else if (protocol.equals("snw")) {
                    		snwTransport.downloadFile(port, command.split(" ")[1], "server_fl");
                    	}
                    	
                    } else if (command.startsWith("get")) {
                    	// Handle file download to client
                    	String fileName = command.split(" ")[1];
                        File file = new File("server_fl/" + fileName);
                        
                        if (file.exists()) {
                        	if (protocol.equals("tcp")) {
                        		dos.writeUTF("File found, starting transfer.");
                        		dos.writeLong(file.length());
                        	
                        	// Send file data to the client
                            try (FileInputStream fis = new FileInputStream(file)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) > 0) {
                                    dos.write(buffer, 0, bytesRead);
                                }
                                dos.flush();
                            }
                            dos.writeUTF("File delivered from server.");
                            
                        	} else if (protocol.equals("snw")) {
                        		String feedback = "File delivered from server.";
                        		// Use Stop-and-Wait protocol to send the file
                        		InetAddress localAddress = clientSocket.getLocalAddress();
                                String clientIP = localAddress.getHostAddress(); 
                                int clientPort = clientSocket.getLocalPort();
                                snwTransport.uploadFile(clientIP, clientPort, file, feedback);
                        	}
                            
                        } else {
                        	System.out.print("File not found in cache or on server. Please check the file name and try again.");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
