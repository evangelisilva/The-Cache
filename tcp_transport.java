import java.io.*;
import java.net.*;

/**
 * The tcp_transport class provides methods for uploading files to a server 
 * using TCP and receiving files on the server.
 */
public class tcp_transport {
	
	// Keep the socket open for use during file upload
    private Socket socket;
	
	
    /**
     * Sends a command to the specified server using a socket connection.
     *
     * @param serverIP   The IP address of the server to connect to.
     * @param serverPort The port number of the server.
     * @param command    The command string to be sent to the server.
     */
    public void sendCommand(String serverIP, int serverPort, String command) {
        try {
            // Create a socket connection to the server using the specified IP and port
            socket = new Socket(serverIP, serverPort);

            // Create a DataOutputStream to send data to the server
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            // Send the command string to the server
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();
            
        } catch (IOException e) {
            System.out.println("Error sending command: " + e);
        }
    }
     
    /**
     * Uploads a file to the server over TCP. Sends the file's name and size, 
     * then streams the file data in chunks. Receives server response on completion.
     *
     * @param serverIP   The IP address of the server.
     * @param serverPort The port number of the server.
     * @param file       The file to be uploaded.
     */
    public void uploadFile(String serverIP, int serverPort, File file) {
        try {
        	// Ensure the socket is connected to the server
            if (socket == null || socket.isClosed()) {
                socket = new Socket(serverIP, serverPort);
            }

            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            // Send the file's name and size to the server
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());

            // Open file input stream and send file data in chunks
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
            }

            dataOutputStream.flush(); // Ensure all data is sent

            System.out.println("Awaiting server response.");
            // Read server's response after file upload
            String response = dataInputStream.readUTF();
            System.out.println("Server response: " + response);

        } catch (EOFException e) {
            System.err.println("End of stream reached unexpectedly: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close(); // Close the socket when done
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
	
    /**
     * Receives a file from a client over TCP, saving it to the server's "server_files" directory.
     *
     * @param dataInputStream  The input stream to read file data from.
     * @param dataOutputStream The output stream to send confirmation to the client.
     * @throws IOException If an error occurs during file reception.
     */
	public void receiveFile(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        try {
        	// Read the file name sent by the client
            String fileName = dataInputStream.readUTF(); 

            // Read the file size sent by the client
            long fileSize = dataInputStream.readLong(); 
            
            // Create new file in 'server_files' directory with received file name
            File file = new File("server_fl", fileName);
            file.getParentFile().mkdirs(); 

            // Write received data to file
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                long totalBytesRead = 0;
                int bytesRead;

                // Continue reading until the total bytes read is less than the file size
                while (totalBytesRead < fileSize) {
                	// Read a chunk of data from the input stream into the buffer
                    bytesRead = dataInputStream.read(buffer);
                    if (bytesRead == -1) break; 
                    
                    // Write the chunk of data to the file
                    fileOutputStream.write(buffer, 0, bytesRead);
                    // Update the total bytes read
                    totalBytesRead += bytesRead;
                }
            }

            // Send a confirmation response back to the client indicating successful upload
            dataOutputStream.writeUTF("File successfully uploaded.");
            System.out.println("File " + fileName + " received.");

        } catch (EOFException e) {
            System.err.println("End of stream reached unexpectedly: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }
	
	/**
     * Downloads a file from the server, saving it in the specified directory on the client-side.
     *
     * @param serverIP The IP address of the server.
     * @param serverPort The port number of the server.
     * @param fileName The name of the file to download.
     * @param dir The directory to save the downloaded file. Defaults to "client_fl" if not specified.
     */
	public void downloadFile(String serverIP, int serverPort, String fileName, String dir) {
	    try {
	    	
	    	// Set default directory if none specified
	    	if (dir == null || dir.isEmpty()) {
	            dir = "client_fl"; 
	        }
	    	
	    	// Establish connection if not already connected
	        if (socket == null || socket.isClosed()) {
	            socket = new Socket(serverIP, serverPort);
	        }

	        DataInputStream dis = new DataInputStream(socket.getInputStream());

	        // Receive initial server response
	        String response = dis.readUTF();

	        // Proceed if file transfer is initiated by the server
	        if (response.equals("File found, starting transfer.")) {
	            // Read the file size
	            long fileSize = dis.readLong();

	            // Set up the file path to save the file
	            File file = new File(dir + "/" + fileName);
	            file.getParentFile().mkdirs();

	            // Write received file data to the file
	            try (FileOutputStream fos = new FileOutputStream(file)) {
	                byte[] buffer = new byte[4096];
	                long totalBytesRead = 0;
	                int bytesRead;

	                // Read exactly 'fileSize' bytes
	                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRead))) > 0) {
	                    fos.write(buffer, 0, bytesRead);
	                    totalBytesRead += bytesRead;
	                }
	            }
	            // After file transfer, read final server message
	            String finalMessage = dis.readUTF();
	            System.out.println("Server response: " + finalMessage);

	        } else {
	            System.out.println("File not found in cache or on server. Please check the file name and try again.");
	        }
	    } catch (EOFException e) {
	        System.err.println("End of stream reached unexpectedly: " + e.getMessage());
	    } catch (IOException e) {
	        System.err.println("Error receiving file: " + e.getMessage());
	    } finally {
	        try {
	            if (socket != null && !socket.isClosed()) {
	                socket.close(); // Close socket after completion
	            }
	        } catch (IOException e) {
	            System.err.println("Error closing socket: " + e.getMessage());
	        }
	    }
	}
    
}
