import java.io.*;
import java.net.*;

/**
 * This class provides methods for file transfer over a UDP connection using
 * Stop-and-Wait protocol. It includes methods to upload and download files with
 * acknowledgement (ACK) and completion (FIN) signals to ensure reliable transfer.
 */
public class snw_transport {
	
	// Protocol handlers for TCP and Stop-and-Wait
    static tcp_transport tcpTransport = new tcp_transport();
	
	/**
	 * Uploads a file to the server using UDP protocol with Stop-and-Wait strategy.
	 * The method sends the file length, waits for an ACK, then sends file chunks.
	 * Each chunk is acknowledged by the server before the next one is sent.
	 * 
	 * @param serverIP   The IP address of the server.
	 * @param serverPort The port number of the server.
	 * @param file       The file to be uploaded.
	 */
	public void uploadFile(String serverIP, int serverPort, File file, String feedback) {
	    try (DatagramSocket socket = new DatagramSocket()) {
	        InetAddress serverAddress = InetAddress.getByName(serverIP);
	        
	        // Prepare the length message to send the file size to the server
	        String lengthMessage = "LEN:" + file.length();
	        byte[] lengthData = lengthMessage.getBytes();
	        boolean ackReceived = false; // Track if acknowledgment is received
	        int retries = 3; // Maximum retries for sending the length message

	        // Retry loop for sending the length message until an ACK is received
	        for (int i = 0; i < retries; i++) {
	            DatagramPacket lengthPacket = new DatagramPacket(lengthData, lengthData.length, serverAddress, serverPort);
	            
	            socket.send(lengthPacket); // Send length packet
	            socket.setSoTimeout(1000); // Set timeout to wait for ACK

	            byte[] ackBuffer = new byte[100]; // Buffer for receiving ACK
	            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	            
	            try {
	                System.out.println("Awaiting server response.");
	                socket.receive(ackPacket); // Wait for ACK from server
	                String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
	                
	                if (ack.equals("ACK")) { // Check if received message is ACK
	                    ackReceived = true; 
	                    break; // Exit loop if ACK is received
	                } else {
	                    System.out.println("Received unexpected message: " + ack);
	                }
	            } catch (SocketTimeoutException e) {
	                System.out.println("Did not receive ACK for LEN. Retrying...");
	                
	                // Add sleep here for a retry pause only if ACK was not received
	                try {
	                    Thread.sleep(5000);
	                } catch (InterruptedException e1) {
	                    e1.printStackTrace();
	                }
	            }
	        }


	        // If ACK is not received after retries, terminate the upload
	        if (!ackReceived) {
	            System.out.println("Failed to receive ACK after " + retries + " attempts. Terminating.");
	            return; 
	        }

	        // Open the file for reading and start sending its contents in chunks
	        try (FileInputStream fis = new FileInputStream(file)) {
	            byte[] buffer = new byte[1000]; // Chunk size for file data
	            int bytesRead;
	            DatagramPacket ackPacket = new DatagramPacket(new byte[100], 100); 
	            
	            // Read and send the file data in chunks
	            while ((bytesRead = fis.read(buffer)) > 0) {
	            	// Create a data packet for each file chunk and send to server
	                DatagramPacket dataPacket = new DatagramPacket(buffer, bytesRead, serverAddress, serverPort);
	                socket.send(dataPacket);

	                // Wait for acknowledgment (ACK) from server for each chunk
	                socket.setSoTimeout(1000); // Timeout for each ACK
	                try {
	                    socket.receive(ackPacket); // Receive ACK for data packet
	                    String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
	                    if (!ack.equals("ACK")) { // Check if ACK is received
	                        throw new IOException("ACK not received for data packet.");
	                    }
	                } catch (SocketTimeoutException e) {
	                    System.out.println("Did not receive ACK for data packet. Terminating.");
	                    return; 
	                }
	            }
	        }
	        
	        // After all chunks are sent, wait for server's FIN message to confirm completion
	        byte[] finBuffer = new byte[100]; // Buffer for receiving FIN message
	        DatagramPacket finPacket = new DatagramPacket(finBuffer, finBuffer.length);
	        
	        try {
                socket.setSoTimeout(1000);
                socket.receive(finPacket); // Receive FIN message
                String finMessage = new String(finPacket.getData(), 0, finPacket.getLength());
                if (finMessage.equals("FIN")) { // Check if received message is FIN
                	byte[] feedbackData = feedback.getBytes();
		        
                	DatagramPacket feedbackPacket = new DatagramPacket(feedbackData, feedbackData.length, serverAddress, serverPort);
                	socket.send(feedbackPacket);
	        	
                	System.out.println("Server response: File successfully uploaded.");
                }
	        } catch (SocketTimeoutException e) {
                System.out.println("Did not receive FIN. Terminating.");
            }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * Downloads a file from a client over UDP using the Stop-and-Wait protocol.
	 * This method listens on the specified port, receives the file length from the client,
	 * sends an acknowledgment (ACK) for each packet received, and saves the file to the specified directory.
	 *
	 * @param port     The port on which the server listens for incoming file data.
	 * @param filename The name to save the downloaded file as.
	 * @param dir      The directory to save the downloaded file. Defaults to "client_fl" if not provided.
	 */
	public void downloadFile(int port, String filename, String dir) {
	    try (DatagramSocket socket = new DatagramSocket(port)) {
	    	
	    	// If directory is null or empty, set default directory
	    	if (dir == null || dir.isEmpty()) {
	            dir = "client_fl";
	        }

	        while (true) {
	        	// Receive LEN message from client indicating the file size
	            byte[] buffer = new byte[1024];
	            DatagramPacket lengthPacket = new DatagramPacket(buffer, buffer.length);
	            socket.receive(lengthPacket);
	            String lengthMessage = new String(lengthPacket.getData(), 0, lengthPacket.getLength());

	            // Check if received message is a LEN message
	            if (lengthMessage.startsWith("LEN:")) {
	                long fileSize = Long.parseLong(lengthMessage.substring(4));

	                // Send ACK for LEN message to client
	                byte[] ackData = "ACK".getBytes();
	                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, lengthPacket.getAddress(), lengthPacket.getPort());
	                socket.send(ackPacket);

	                // Prepare to receive file data
	                File file = new File(dir, filename);
	                file.getParentFile().mkdirs(); // Create directories if needed

	                try (FileOutputStream fos = new FileOutputStream(file)) {
	                    long totalBytesReceived = 0;

	                    // Receive file data packets until the entire file is received
	                    while (totalBytesReceived < fileSize) {
	                        DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
	                        socket.receive(dataPacket);

	                        // Write received data to file
	                        fos.write(dataPacket.getData(), 0, dataPacket.getLength());
	                        totalBytesReceived += dataPacket.getLength();

	                        // Send ACK for each data packet
	                        byte[] ackDataForData = "ACK".getBytes();
	                        DatagramPacket ackPacketForData = new DatagramPacket(ackDataForData, ackDataForData.length, dataPacket.getAddress(), dataPacket.getPort());
	                        socket.send(ackPacketForData);
	                    }

	                    // After receiving the entire file, send FIN to the client
	                    byte[] finData = "FIN".getBytes();
	                    DatagramPacket finPacket = new DatagramPacket(finData, finData.length, lengthPacket.getAddress(), lengthPacket.getPort());
	                    socket.send(finPacket);
	                	// Protocol handlers for TCP and Stop-and-Wait
	                    buffer = new byte[1024];
	    	            DatagramPacket feedbackPacket = new DatagramPacket(buffer, buffer.length);
	    	            socket.receive(feedbackPacket);
	    	            String feedbackMessage = new String(feedbackPacket.getData(), 0, feedbackPacket.getLength());
	    	            if (feedbackMessage != "")
	    	            	System.out.println("Server Response: " + feedbackMessage);
	                } catch (IOException e) {
	                    System.err.println("Error while receiving the file: " + e.getMessage());
	                }
	            }

	            break; // Break the loop after one file transfer
	        }
	    } catch (IOException e) {
	        System.err.println("Error in server: " + e.getMessage());
	    }
	}

}

