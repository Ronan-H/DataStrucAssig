package ie.gmit.sw;

import java.util.Random;
import java.util.Scanner;

public class Runner {
	private Cipher cipher;
	private Scanner console;
	
	public Runner() {
		console = new Scanner(System.in);
	}
	
	public void start() {
		int i;
		int choice;
		Random random;
		char genChar;
		String key = null;
		int numKeys;
		StringBuilder[] inputKeys = {null, null};
		KeySanitizer keySanitizer;
		Cipher cipher;
		boolean readFromFile;
		
		System.out.println();
		System.out.println("==============================");
		System.out.println("|  Rapid Four Square Cipher  |");
		System.out.println("|       By Ronan Hanley      |");
		System.out.println("==============================");
		System.out.println();
		
		System.out.println("Choose a source for the cipher key");
		choice = getUserOption("Randomly generate a key for me", "Let me enter the key");
		
		switch (choice) {
		case 1:
			// randomly generate the key
			key = Cipher.generateKey();
			break;
		case 2:
			// get key by user input
			System.out.println("Use one key or two?");
			System.out.printf("\t(One key max length: %d%n", Cipher.ALPHABET_SIZE);
			System.out.printf("\t Two keys max length each: %d%n", Cipher.ALPHABET_SIZE /2);
			System.out.println("\t smaller keys will be automatically padded)");
			
			numKeys = getUserOption("I want one key", "I want to enter two keys");
			
			for (i = 0; i < numKeys; ++i) {
				System.out.printf("\nPlease enter key %d:\n> ", (i + 1));
				inputKeys[i] = new StringBuilder(console.nextLine());
			}
			
			key = new KeySanitizer(inputKeys).getSanitizedKey();
			System.out.printf("DEBUG: Key is %d chars long.%n", key.length());
		}
		
		System.out.printf("Key: %s\n", key);
		System.out.println("Initializing cipher...");
		cipher = new Cipher(key);
		System.out.println("Finished initializing cipher.");
		
		System.out.println("Read input data from file or URL?");
		choice = getUserOption("Read from file", "Read from URL");
		
		long start = System.nanoTime();
		String fileName = "PoblachtNaHEireann";
		cipher.processFile(fileName, true, true); // encrypt
		// cipher.processFile(fileName, false, true); // decrypt
		System.out.printf("Time taken: %.2fms.\n", (System.nanoTime() - start) / 1000000f);
	}
	
	public int getUserOption(String...options) {
		int i;
		
		System.out.println("\nSelect an option:");
		for (i = 0; i < options.length; ++i) {
			System.out.printf("[%d]: %s\n", (i + 1), options[i]);
		}
		
		System.out.print("\n");
		
		return getValidatedInt(1, options.length);
	}
	
	public int getValidatedInt(int min, int max) {
		int input = 0;
		boolean valid;
		
		do {
			System.out.print("> ");
			try {
				input = Integer.parseInt(console.nextLine());
				valid = (input >= min && input <= max);
			} catch(NumberFormatException e) {
				valid = false;
			}
			
			if (!valid) {
				System.out.printf("Invalid input; must be a number between %d and %d (inclusive).\n", min, max);
				System.out.println("Please try again.");
			}
		} while(!valid);
		
		return input;
	}
	
	public static void main(String[] args) {
		Runner runner = new Runner();
		runner.start();
	}
	
}
