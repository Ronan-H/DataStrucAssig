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
			random = new Random();
			StringBuilder keyBuilder = new StringBuilder(64);
			
			for (i = 0; i < 64; ++i) {
				// TODO Generate UNIQUE chars!!
				genChar = (char)Cipher.UNPACKED_CHARS[random.nextInt(64)];
				keyBuilder.append(genChar);
			}
			
			System.out.println("Generated key: " + keyBuilder.toString());

			break;
		case 2:
			// get key by user input
			
		}
		
		System.out.print("Enter an key to use in the cipher for encryption/decryption:\n> ");
		
		
		cipher = new Cipher("lps8H T4naht2FdGPBw5SYV,Nx0XfboOACyz1jMZKir6W9uQIqeJ7UgmELcD3Rkv"
						  + "mgjFuYZ7BEIORpzGNxUTyn0rSWJ Xfv,PesKQV96L5tihCloc3w8A2HqD1ka4dbM");

		long start = System.nanoTime();
		String fileName = "WarAndPeace-LeoTolstoy";
		// processFile(fileName, true); // encrypt
		cipher.processFile(fileName, false, true); // decrypt
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
