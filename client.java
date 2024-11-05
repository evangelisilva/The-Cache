import java.io.*;
import java.util.*;

/**
 * The Client class provides a console-based interface for interacting with a server
 * to upload and download files using specified transport protocols (TCP or SNW).
 * 
 * Supported commands:
 *  - "put <filename>": Uploads a specified file to the server.
 *  - "get <filename>": Downloads a specified file from the server or cache.
 *  - "quit": Exits the program.
 *  
 * Usage:
 *  java client <server-ip> <server-port> <cache-ip> <cache-port> <protocol>
 *  
 * Arguments:
 *  - <server-ip>: IP address of the server.
 *  - <server-port>: Port number of the server.
 *  - <cache-ip>: IP address of the cache server.
 *  - <cache-port>: Port number of the cache server.
 *  - <protocol>: Transport protocol to use (either "tcp" or "snw").
 */
public class client {
    
	public static void main(String[] args) throws IOException {
        
		// Verify that the correct number of command-line arguments is provided
		if (args.length != 5) {
			System.out.println("Usage: java client <server-ip> <server-port> <cache-ip> <cache-port> <protocol>");
			return;
		}
        
        // Extract command-line arguments for server IP, port, cache IP, port, and protocol type
        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String cacheIP = args[2];
        int cachePort = Integer.parseInt(args[3]);
        String protocol = args[4].toLowerCase();
        
        if (!protocol.equals("tcp") &&  !protocol.equals("snw")) {
        	System.out.println("Error: Invalid protocol");
        	return;
        }
        
        // Initialize transport protocol handlers (TCP and SNW protocols)
        tcp_transport tcpTransport = new tcp_transport();
        snw_transport snwTransport = new snw_transport(); 
        
        // Initialize Scanner to capture user input
        Scanner scanner = new Scanner(System.in);
        
        
        File directory = new File("client_fl");
        if (!directory.exists()) 
        	directory.mkdirs();
        
        
        while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine();
            String[] parts = command.split(" ");
            
            // Exit the program if the command is "quit"
            if (parts[0].equals("quit")) {
                System.out.println("Exiting program!");
                break;
                
            // Handle 'put' command to upload a file to the server 
            } else if (parts[0].equals("put") || parts[0].equals("get")) {

            	if (parts[0].equals("put")) { 
                    if (parts.length == 2) {
                    	// Send 'put' command to the server
                    	tcpTransport.sendCommand(serverIP, serverPort, command);
                    	
                        String filePath = parts[1];
                        File file = new File("client_fl", filePath);
                        
                        // Verify that the file exists
                        if (!file.exists()) {
                            System.out.println("File not found: " + filePath);
                        } else {
                            
                        	// Choose protocol to upload the file (TCP or Stop-and-Wait over UDP)
                            if (protocol.equals("tcp")) {
                                tcpTransport.uploadFile(serverIP, serverPort, file);
                            } else if (protocol.equals("snw")) {
                                snwTransport.uploadFile(serverIP, serverPort, file, "");
                            } else {
                            	// Display an error if an unsupported protocol is provided
                                System.out.println("Unsupported protocol: " + protocol);
                            }
                        }
                    } else {
                    	// Display correct usage format if command format is incorrect
                        System.out.println("Invalid command format. Usage: put <filename>");
                    }
                    
                // Handle 'get' command to download a file from the cache
            	} else if (parts[0].equals("get")){
                	if (parts.length == 2) {
                		// Send 'get' command to the cache
                		tcpTransport.sendCommand(cacheIP, cachePort, command);
                		
                		// Choose protocol to download the file (TCP or Stop-and-Wait over UDP)
                		if (protocol.equals("tcp")) {
                			tcpTransport.downloadFile(cacheIP, cachePort, parts[1], null);
                		} else if (protocol.equals("snw")) {
                			snwTransport.downloadFile(cachePort, parts[1], null);
                		}
                	} else {
                		// Display correct usage format if command format is incorrect
                		System.out.println("Invalid command format. Usage: get <filename>");
                    }
                }
            } else {
                System.out.println("Invalid command.");
            }

        }
        
        scanner.close();
    }
}
