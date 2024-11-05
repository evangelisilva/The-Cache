import java.io.*;
import java.net.*;

/**
 * The Cache class functions as an intermediary between clients and the main server.
 * It stores frequently requested files and serves them to clients, reducing load on the main server.
 * 
 * This class supports both TCP and Stop-and-Wait (SNW) protocols for file transfer.
 * 
 * Arguments:
 *  - <cache-port>: Port number for the cache server.
 *  - <server-ip>: IP address of the main server.
 *  - <server-port>: Port number of the main server.
 *  - <protocol>: Transport protocol ("tcp" or "snw") to be used for file transfer.
 */
public class cache {

	// Protocol handlers for TCP and Stop-and-Wait
    static tcp_transport tcpTransport = new tcp_transport();
    static snw_transport snwTransport = new snw_transport();

    public static void main(String[] args) {
    	
    	if (args.length != 4) {
			System.out.println("Usage: java cache <cache-ip> <cache-port> <server-port> <protocol>");
			return;
		}
    	
        int cachePort = Integer.parseInt(args[0]);
        String serverIP = args[1];
        int serverPort = Integer.parseInt(args[2]);
        String protocol = args[3].toLowerCase();
        
        if (!protocol.equals("tcp") &&  !protocol.equals("snw")) {
        	System.out.println("Error: Invalid protocol");
        	return;
        }
        
        File directory = new File("cache_fl");
        if (!directory.exists()) 
        	directory.mkdirs();

        try (ServerSocket cacheSocket = new ServerSocket(cachePort)) {
            System.out.println("cache started on port " + cachePort);

            // Continuously listen for client requests
            while (true) {
                try (Socket clientSocket = cacheSocket.accept();
                     DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                    String command = dis.readUTF();

                    if (command.startsWith("get")) {
                        String fileName = command.split(" ")[1];
                        File file = new File("cache_fl/" + fileName);

                        // If file exists in cache, serve it directly to the client
                        if (file.exists()) {
                            if (protocol.equals("tcp")) {
                                dos.writeUTF("File found, starting transfer.");
                                
                                // Send the file size first
                                dos.writeLong(file.length());
                                
                                // Transfer file data to client
                                try (FileInputStream fis = new FileInputStream(file)) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;

                                    while ((bytesRead = fis.read(buffer)) > 0) {
                                        dos.write(buffer, 0, bytesRead);
                                    }
                                    dos.flush();
                                }
                                
                                dos.writeUTF("File delivered from cache.");
                                dos.flush();
                                
                            } else if (protocol.equals("snw")) {
                            	String feedback = "File delivered from cache.";
                            	
                            	// Use Stop-and-Wait protocol to send the file
                            	InetAddress localAddress = clientSocket.getLocalAddress();
                                String clientIP = localAddress.getHostAddress(); 
                                int clientPort = clientSocket.getLocalPort();
                                snwTransport.uploadFile(clientIP, clientPort, file, feedback);
                                
                                dos.writeUTF(feedback);
                                dos.flush();
                            }
                            
                        } else {
                        	// File not found in cache, request it from main server
                        	tcpTransport.sendCommand(serverIP, serverPort, command);
                        	
                        	if (protocol.equals("tcp")) {
                        		// Download file using TCP protocol
                        		tcpTransport.downloadFile(serverIP, serverPort, fileName, "cache_fl");
                        	
                        		// After downloading, serve the file to the client
                        		if (file.exists()) {
                        			dos.writeUTF("File found, starting transfer.");
                                    
                                    dos.writeLong(file.length()); // Send file size
                                    
                                    try (FileInputStream fis = new FileInputStream(file)) {
                                        byte[] buffer = new byte[4096];
                                        int bytesRead;

                                        while ((bytesRead = fis.read(buffer)) > 0) {
                                            dos.write(buffer, 0, bytesRead);
                                        }
                                        dos.flush();
                                    }
                                    
                                    dos.writeUTF("File delivered from server.");
                                    dos.flush();
                                    System.out.println("File " + fileName + " sent from server.");
                                } else {
                                	dos.writeUTF("File not found in cache or on server. Please check the file name and try again.");
                                    dos.flush();
                                }
                        		
                            } else if (protocol.equals("snw")) {
                            	// Download file using SNW protocol and then serve to client
                            	snwTransport.downloadFile(serverPort, fileName, "cache_fl");
                            	
                            	if (file.exists()) {
                            		String feedback = "File delivered from server.";
                            		
                            		InetAddress localAddress = clientSocket.getLocalAddress();
                                    String clientIP = localAddress.getHostAddress(); 
                                    int clientPort = clientSocket.getLocalPort();
                                    snwTransport.uploadFile(clientIP, clientPort, file, feedback);
                                    
                                    dos.writeUTF(feedback);
                                    dos.flush();
                            	}
                            	else {
                                	dos.writeUTF("File not found in cache or on server. Please check the file name and try again.");
                                    dos.flush();
                                }
                        		
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error processing client request: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Cache server error: " + e.getMessage());
        }
    }  
    
}
