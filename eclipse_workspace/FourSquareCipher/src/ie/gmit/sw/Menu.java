package ie.gmit.sw;

import static java.lang.System.out;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A command line driven menu.
 */
public final class Menu {
	private Scanner console;
	private boolean running;
	
	public Menu() {
		console = new Scanner(System.in);
	}
	
	/**
	 * (too complicated for Big O)
	 */
	public void go() {
		running = true;
		
		int choice;
		Cipher cipher;
		int fileChooseResult;
		boolean readFromURL;
		boolean encryptMode;
		boolean writeToFile;
		final String defaultInputFile = "./input/WarAndPeace-LeoTolstoy.txt";
		URL inputURL = null;
		boolean validURL;
		File resource;
		String resourcePath;
		long timerStart;
		double msTaken;
		
		// (this anonymous inner class taken from StackOverflow in an attempt to
		// ensure the file chooser appears on top of all other windows)
		JFileChooser fileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;
			@Override
			protected JDialog createDialog(Component parent) throws HeadlessException {
				JDialog dialog = super.createDialog(parent);
				dialog.setLocationByPlatform(true);
				dialog.setAlwaysOnTop(true);
				return dialog;
			}
		};
		// -----
		
		// only show text files to the user when they are selecting an input file
		FileFilter fileFilter = new FileNameExtensionFilter("Text files", "txt");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(fileFilter);
		fileChooser.setDialogTitle("Select an input file");
		
		// print the program header
		out.println();
		out.println(" ==============================");
		out.println(" |     Four Square Cipher     |");
		out.println(" |      By Ronan Hanley       |");
		out.println(" ==============================");
		
		// Get a key for the cipher and initialize it right away, since the
		// application is useless without one.
		cipher = initCipher();
		
		while (running) {
			// print main menu and get the user's option choice
			choice = getUserOption("Encrypt", "Decrypt", "Change the key", "Print cipher key & four squares", "Exit");
			
			switch (choice) {
			// encryption an decryption roll into the same branch of code
			case 1:
			case 2:
				// set mode based on user's choice
				encryptMode = (choice == 1);
				
				readFromURL = getUserOption("Read from a file", "Read from a URL") == 2;
				
				if (readFromURL) {
					// read from URL
					// do..while input validation for URLs
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
						// set the input folder as the initial file chooser directory
						fileChooser.setCurrentDirectory(new File("./input/"));
						fileChooser.setSelectedFile(new File(defaultInputFile));
					}
					else {
						// Decrypt mode
						
						// set the out folder as the initial file chooser directory
						// (since the user will probably want to decrypt previously
						// encrypted text)
						fileChooser.setCurrentDirectory(new File("./output/"));
						fileChooser.setSelectedFile(new File(""));
					}
					
					out.println("Showing file chooser window, please choose a file.");
					out.println("(it might be hidden behind some windows)\n");
					fileChooseResult = fileChooser.showOpenDialog(null);
					fileChooser.requestFocusInWindow();
					
					if (fileChooseResult != JFileChooser.APPROVE_OPTION) {
						// no file selected; return to main menu
						continue;
					}
					
					resource = fileChooser.getSelectedFile();
					resourcePath = resource.getAbsolutePath();
					
					// ensure the file exists, and the user has permission to write to it
					if (!resource.exists()) {
						out.println("Selected file does not exist!\n");
						continue;
					}
					
					if (!resource.canWrite()) {
						out.println("Insufficient permissions to write to chosen file!\n");
						continue;
					}
				}
				
				writeToFile = (getUserOption("Print output to a file", "Print output to the console") == 1);
				
				if (writeToFile) {
					// let the user know where the output file will go
					out.printf("(file will be written to the \"output\" folder of this project, with the extension %s appended)%n%n",
								(encryptMode ? "_enc" : "_dec"));
				}
				
				out.printf("%s data...%n%s",
							(encryptMode ? "Encrypting" : "Decrypting"),
							(writeToFile ? "" : "\n")); // make some more space if we're writing to the console
				// start the timer to measure how long encryption/decryption takes
				timerStart = System.nanoTime();
				try {
					cipher.processFile(resourcePath, encryptMode, readFromURL, writeToFile);
					msTaken = (System.nanoTime() - timerStart) / 1000000f;
					out.println("\nFinished.\n");
					out.printf("Contents read, encrypted, and written in: %.2fms.%n%n", msTaken);
				}
				catch (FileNotFoundException e) {
					System.err.println("Error while processing file:\n");
					e.printStackTrace(System.out);
				}
				catch (UnknownHostException e) {
					out.printf("Unknown host \"%s\"%n%n", resourcePath);
				}
				catch (IOException e) {
					System.err.print("Error occured while trying to process the input file/URL!\n\n");
					e.printStackTrace(System.out);
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
				out.println("\n\n(new lines are represented as the character \'^\')");
				break;
			case 5:
				// Exit
				System.exit(0);
			}
		}
	}
	
	/**
	 * Running time: O(n)
	 * Reasoning: Runtime grows with how many options there are to print.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Some extra variables.
	 * 
	 * 
	 * 
	 * Prints a list of options to the screen, along with their option number.
	 * Returns the user's validated option choice.
	 */
	private int getUserOption(String...options) {
		int i;
		
		out.println("\nSelect an option:");
		// print the options
		for (i = 0; i < options.length; ++i) {
			out.printf("[%d]: %s\n", (i + 1), options[i]);
		}
		
		out.print("\n");
		
		// return the user's validated choice
		return getValidatedInt(1, options.length);
	}
	
	/**
	 * Running time: Unknown
	 * Reasoning: Depends on how many times the user enters invalid input.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Some extra variables.
	 * 
	 * 
	 * 
	 * Repetitively asks the user between min and max until they supply it.
	 */
	private int getValidatedInt(int min, int max) {
		int input = 0;
		boolean valid;
		
		do {
			out.print("> ");
			try {
				input = Integer.parseInt(console.nextLine());
				valid = (input >= min && input <= max);
			} catch(NumberFormatException e) {
				// input wasn't a number
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
	
	/**
	 * Running time: O(1)
	 * Reasoning: No loops.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Some extra variables.
	 * 
	 * 
	 * 
	 * Initialises the Cipher based on user input.
	 */
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
			
			// get an input string for each key
			for (i = 0; i < numKeys; ++i) {
				out.printf("\nEnter key %d of %d (use \'^\' for the newline character):\n> ", (i + 1), numKeys);
				inputKeys[i] = new StringBuilder(console.nextLine().replace('^', '\n'));
			}
			out.println();
			
			// convert whatever the user entered into a derived usable Cipher key
			key = new KeySanitiser(inputKeys).getSanitizedKey();
		}
		
		// create the Cipher object and time how long the initialisation takes
		out.println("Initializing cipher...");
		timerStart = System.nanoTime();
		cipher = new Cipher(key);
		out.printf("Cipher initialized in: %.2fms.%n", (System.nanoTime() - timerStart) / 1000000f);
		
		return cipher;
	}
	
}
