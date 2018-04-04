package ie.gmit.sw;

import static java.lang.System.out;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class Menu {
	public static final String SSD_ROOT = ".";
	public static final String HDD_ROOT = "G:/FourSquareCipher";
	public static final String ROOT_DIR = SSD_ROOT;
	
	private Scanner console;
	private boolean running;
	
	public Menu() {
		console = new Scanner(System.in);
	}
	
	public void go() {
		running = true;
		
		int choice;
		Cipher cipher;
		boolean readFromURL;
		int fileChooseResult;
		boolean encryptMode;
		boolean writeToFile;
		final String defaultInputFile = ROOT_DIR + "/input/WarAndPeace-LeoTolstoy.txt";
		String startingDir;
		URL inputURL = null;
		boolean validURL;
		File resource;
		String resourcePath;
		
		JFileChooser fileChooser = new JFileChooser();
		FileFilter fileFilter = new FileNameExtensionFilter("Text files", "txt");
		
		long timerStart;
		double msTaken;
		
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(fileFilter);
		fileChooser.setDialogTitle("Select an input file");
		
		out.println();
		out.println(" ==============================");
		out.println(" |     Four Square Cipher     |");
		out.println(" |      By Ronan Hanley       |");
		out.println(" ==============================");
		
		/* Get a key for the cipher and initialize it right away, since the
		 * application is useless without one.
		 */
		cipher = initCipher();
		
		while (running) {
			choice = getUserOption("Encrypt", "Decrypt", "Change the key", "Print cipher key & four squares", "Exit");
			
			switch (choice) {
			case 1:
			case 2:
				// Encrypt/Decrypt
				encryptMode = (choice == 1);
				
				readFromURL = (getUserOption("Read from a file", "Read from a URL") == 2);
				
				if (readFromURL) {
					// read from url
					do {
						out.print("Enter a URL to read from\n\n> ");
						try {
							inputURL = new URL(console.nextLine());
							validURL = true;
						} catch (MalformedURLException e) {
							out.println("That URL is not valid. Please try again.\n");
							validURL = false;
						}
					} while(!validURL);
					
					resourcePath = inputURL.toString();
				}
				else {
					// read from file
					if (encryptMode) {
						fileChooser.setCurrentDirectory(new File(ROOT_DIR + "/input/"));
						fileChooser.setSelectedFile(new File(defaultInputFile));
					}
					else {
						// decrypt mode
						fileChooser.setCurrentDirectory(new File(ROOT_DIR + "/output/"));
						fileChooser.setSelectedFile(new File(""));
					}
					
					out.println("Showing file chooser window, please choose a file.");
					out.println("(it might be hidden behind some windows)\n");
					fileChooseResult = fileChooser.showOpenDialog(null);
					fileChooser.requestFocusInWindow();
					
					if (fileChooseResult != JFileChooser.APPROVE_OPTION) {
						continue;
					}
					
					resource = fileChooser.getSelectedFile();
					resourcePath = resource.getAbsolutePath();
					
					if (!resource.exists()) {
						System.out.println("Selected file does not exist!\n");
						continue;
					}
					
					if (!resource.canWrite()) {
						System.out.println("Insufficient permissions to write to chosen file!\n");
						continue;
					}
				}
				
				writeToFile = (getUserOption("Print output to a file", "Print output to the console") == 1);
				
				if (writeToFile) out.println("(file will be written to the \"output\" folder of this project)\n");
				
				System.out.printf("%s file...%n", (encryptMode ? "Encrypting" : "Decrypting"));
				timerStart = System.nanoTime();
				try {
					cipher.processFile(resourcePath, encryptMode, readFromURL, writeToFile);
					msTaken = (System.nanoTime() - timerStart) / 1000000f;
					System.out.println("\nFinished.\n");
					out.printf("Contents read, encrypted, and written in: %.2fms.%n%n", msTaken);
				}
				catch (FileNotFoundException e) {
					System.err.println("Error while processing file:\n");
					e.printStackTrace();
				}
				catch (UnknownHostException e) {
					out.printf("Unknown host \"%s\"%n%n", resourcePath);
				}
				catch (IOException e) {
					System.err.print("Error occured while trying to process the input file/URL!\n\n");
					e.printStackTrace();
				}
				break;
			case 3:
				// Change the key
				cipher = initCipher();
				break;
			case 4:
				// Print cipher key & four squares
				cipher.printKey();
				cipher.printSquares();
				System.out.println("\n\n(new lines are represented as the character \'^\')");
				break;
			case 5:
				// Exit
				System.exit(0);
			}
		}
		
		out.println("Read input data from file or URL?");
		choice = getUserOption("Read from file", "Read from URL");
	}
	
	private int getUserOption(String...options) {
		int i;
		
		out.println("\nSelect an option:");
		for (i = 0; i < options.length; ++i) {
			out.printf("[%d]: %s\n", (i + 1), options[i]);
		}
		
		out.print("\n");
		
		return getValidatedInt(1, options.length);
	}
	
	private int getValidatedInt(int min, int max) {
		int input = 0;
		boolean valid;
		
		do {
			out.print("> ");
			try {
				input = Integer.parseInt(console.nextLine());
				valid = (input >= min && input <= max);
			} catch(NumberFormatException e) {
				valid = false;
			}
			
			out.println();
			
			if (!valid) {
				out.printf("Invalid input; must be a number between %d and %d (inclusive).\n", min, max);
				out.println("Please try again.\n");
			}
		} while(!valid);
		
		return input;
	}
	
	private Cipher initCipher() {
		int i;
		int choice;
		String key = null;
		int numKeys;
		StringBuilder[] inputKeys = {null, null};
		Cipher cipher;
		long timerStart;
		
		choice = getUserOption("Randomly generate the cipher key", "Let me enter the cipher key");
		
		switch (choice) {
		case 1:
			// randomly generate the key
			key = Cipher.generateRandomKey();
			break;
		case 2:
			// get key by user input
			out.println("Use one key or two?");
			
			numKeys = getUserOption("I want one key", "I want to enter two keys");
			
			for (i = 0; i < numKeys; ++i) {
				out.printf("Enter key %d of %d (use \'^\' for the newline character):\n> ", (i + 1), numKeys);
				inputKeys[i] = new StringBuilder(console.nextLine().replace('^', '\n'));
			}
			
			key = new KeySanitizer(inputKeys).getSanitizedKey();
		}
		
		out.println("Initializing cipher...");
		timerStart = System.nanoTime();
		cipher = new Cipher(key);
		out.printf("Cipher initialized in: %.2fms.%n", (System.nanoTime() - timerStart) / 1000000f);
		
		return cipher;
	}
	
}
