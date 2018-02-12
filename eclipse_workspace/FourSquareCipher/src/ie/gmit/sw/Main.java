package ie.gmit.sw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Main {
	private static Cipher cipher;
	private static final int BUFFER_LEN = 16384;
	private static BufferedOutputStream out;
	private static int outputCounter;
	private static byte[] outputBytes;
	
	private static void writeBytes(byte[] processedBytes) throws IOException {
		if (outputCounter == BUFFER_LEN -1) {
			outputBytes[outputCounter++] = processedBytes[0];
			
			writeIfFull();
			
			outputBytes[outputCounter++] = processedBytes[1];
		}
		else {
			outputBytes[outputCounter++] = processedBytes[0];
			outputBytes[outputCounter++] = processedBytes[1];
		}
	}
	

	private static void writeBytes(byte[] processedBytes, List<Byte> carriedChars) throws IOException {
		outputBytes[outputCounter++] = processedBytes[0];
		
		writeIfFull();
		
		for (int j = 0; j < carriedChars.size(); ++j) {
			outputBytes[outputCounter++] = carriedChars.get(j);
			writeIfFull();
		}
		
		outputBytes[outputCounter++] = processedBytes[1];
	}
	
	private static void writeIfFull() throws IOException {
		if (outputCounter == BUFFER_LEN) {
			out.write(outputBytes);
			outputBytes = new byte[BUFFER_LEN];
			outputCounter = 0;
		}
	}
	
	public static void processFile(String fileName, boolean encryptMode) {
		try {
			String inputPath = String.format("%s/%s%s.txt",
				(encryptMode ? "input" : "output"),
				fileName,
				(encryptMode ? "" : "_enc"));
			
			String outputPath = String.format("output/%s%s.txt",
					fileName,
					(encryptMode ? "_enc" : "_dec"));
			
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputPath));
			out = new BufferedOutputStream(new FileOutputStream(outputPath));
			
			byte[] inputBytes = new byte[BUFFER_LEN];
			int bytesRead;
			byte bigramA = 0, bigramB;
			boolean charStored = false;
			byte b;
			byte packedByte;
			byte[] processedBytes = new byte[2];
			// intial size for the carriedChars ArrayList
			final int CARRIED_CHARS_INITIAL_SIZE = 5;
			List<Byte> carriedChars = null;
			
			outputBytes = new byte[BUFFER_LEN];
			outputCounter = 0;
			
			while ((bytesRead = in.read(inputBytes)) != -1) {
				for (int i = 0; i < bytesRead; ++i) {
					b = inputBytes[i];
					
					packedByte = cipher.packedChars[b];
					
					if (packedByte != -1) {
						// byte should be encrypted
						if (charStored) {
							bigramB = packedByte;
							
							if (encryptMode) {
								processedBytes = cipher.encryptPackedChars(bigramA, bigramB);	
							}
							else {
								processedBytes = cipher.decryptPackedChars(bigramA, bigramB);
							}
							
							if (carriedChars == null) {
								writeBytes(processedBytes);
							}
							else {
								writeBytes(processedBytes, carriedChars);
								carriedChars = null;
							}
							
							charStored = false;
						}
						else {
							bigramA = packedByte;
							charStored = true;
						}
					}
					else {
						// packedByte == -1
						if (charStored) {
							if (carriedChars == null) {
								// start a buffer of unsupported chars, to be written later
								carriedChars = new ArrayList<Byte>(CARRIED_CHARS_INITIAL_SIZE);
							}
							
							carriedChars.add(b);
						}
						else {
							outputBytes[outputCounter++] = b;
						}
					}
					
					writeIfFull();
				}
			}
			
			in.close();
			
			/*
			System.out.println("Final bytes read: " + finalBytesRead);
			for (int i = 0; i < outputBytes.length; ++i) {
				System.out.printf("Byte %d: %d\n", i, outputBytes[i]);
			}
			*/
			
			out.write(outputBytes, 0, outputCounter);
			if (charStored) {
				System.out.println("First char: " + (char)cipher.unpackedChars[bigramA]);
				
				bigramB = cipher.packedChars[(short) ' '];
				
				if (encryptMode) {
					processedBytes = cipher.encryptPackedChars(bigramA, bigramB);	
				}
				else {
					processedBytes = cipher.decryptPackedChars(bigramA, bigramB);
				}
				
				out.write(processedBytes[0]);
				out.write(processedBytes[1]);
				
				if (carriedChars != null) {
					for (int i = 0; i < carriedChars.size(); ++i) {
						out.write(carriedChars.get(i));
					}
					
					carriedChars = null;
				}
			}
			
			if (carriedChars != null) {
				for (int i = 0; i < carriedChars.size(); ++i) {
					out.write(carriedChars.get(i));
				}
			}
			
			out.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		cipher = new Cipher("lps8H T4naht2FdGPBw5SYV,Nx0XfboOACyz1jMZKir6W9uQIqeJ7UgmELcD3Rkv"
								 + "mgjFuYZ7BEIORpzGNxUTyn0rSWJ Xfv,PesKQV96L5tihCloc3w8A2HqD1ka4dbM");
		
		// cipher.printSquares();
		
		/*
		byte[] enBytes = cipher.decryptChars((byte) 'T', (byte) '5');
		char[] enChars = {(char)enBytes[0], (char)enBytes[1]};
		System.out.println("Chars: " + enChars[0] + " " + enChars[1]);
		*/
		
		long start = System.nanoTime();
		String fileName = "WarAndPeace-LeoTolstoy";
		processFile(fileName, true);
		// processFile(fileName, false);
		System.out.printf("Time taken: %.2fms.\n", (System.nanoTime() - start) / 1000000f);
	}
	
}
