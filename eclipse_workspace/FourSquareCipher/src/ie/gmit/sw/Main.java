package ie.gmit.sw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
	private static Cipher cipher;
	private static final int BUFFER_LEN = 16384;
	private static BufferedOutputStream out;
	private static int outputCounter;
	private static byte[] outputBytes;
	
	private static void writeIfFull() throws IOException {
		if (outputCounter >= BUFFER_LEN) {
			out.write(outputBytes);
			outputBytes = new byte[BUFFER_LEN];
			outputCounter = 0;
		}
	}
	
	public static void readFile(String fileName, boolean encryptMode) {
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
			int finalBytesRead = 0;
			byte bigramA = 0, bigramB;
			boolean charStored = false;
			byte b;
			byte packedByte;
			byte[] processedBytes = new byte[2];
			
			outputBytes = new byte[BUFFER_LEN];
			outputCounter = 0;
			
			while ((bytesRead = in.read(inputBytes)) != -1) {
				if (bytesRead != -1) {
					finalBytesRead = bytesRead;
				}
				
				for (int i = 0; i < inputBytes.length; ++i) {
					b = inputBytes[i];
					
					packedByte = cipher.packedChars[b];
					
					if (packedByte != -1) {
						if (charStored) {
							bigramB = packedByte;
							
							if (encryptMode) {
								processedBytes = cipher.encryptPackedChars(bigramA, bigramB);	
							}
							else {
								processedBytes = cipher.decryptPackedChars(bigramA, bigramB);
							}
							
							
							if (outputCounter == BUFFER_LEN -1) {
								outputBytes[outputCounter] = processedBytes[0];
								
								writeIfFull();
								
								outputBytes[outputCounter++] = processedBytes[1];
							}
							else {
								outputBytes[outputCounter++] = processedBytes[0];
								outputBytes[outputCounter++] = processedBytes[1];
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
						outputBytes[outputCounter++] = b;
					}
					
					writeIfFull();
				}
			}
			
			out.write(outputBytes, 0, finalBytesRead);
			
			out.flush();
			in.close();
			out.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		cipher = new Cipher("lps8H T4naht2FdGPBw5SYV,Nx0XfboOACyz1jMZKir6W9uQIqeJ7UgmELcD3Rkv"
								 + "mgjFuYZ7BEIORpzGNxUTyn0rSWJ Xfv,PesKQV96L5tihCloc3w8A2HqD1ka4dbM");
		
		// cipher.printSquares();
		
		byte[] enBytes = cipher.encryptChars((byte) 'T', (byte) 'E');
		char[] enChars = {(char)enBytes[0], (char)enBytes[1]};
		
		long start = System.nanoTime();
		readFile("WarAndPeace-LeoTolstoy", true);
		readFile("WarAndPeace-LeoTolstoy", false);
		System.out.printf("Time taken: %.2fms.\n", (System.nanoTime() - start) / 1000000f);
	}
	
}
