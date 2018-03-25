package ie.gmit.sw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class Cipher {
	// number of characters which can be encrypted,
	// i.e. number of characters in 1 quadrant of the four squares
	public static final int ALPHABET_SIZE = 64;
	public static final int SQUARED_ALPHABET_SIZE = (int)Math.pow(ALPHABET_SIZE, 2);
	public static final int SQRT_ALPHABET_SIZE = (int)Math.sqrt(ALPHABET_SIZE);
	
	// 3d array (3x 2 dim), containing the characters in the four squares,
	// in "packed" form.
	private final byte[][][] fourSq;
	// a lookup table to convert a java char to a "packed" char:
	// a packed char is 6 bits, with 0 being A, B being 1, all the
	// way up to 63 -> ','
	public static final byte[] PACKED_CHARS;
	// the reverse of the above (converts packed char back to java char)
	public static final short[] UNPACKED_CHARS;
	// Another lookup table for encryption. Usage explained in the README.txt
	private final short[] encryptArr;
	// the reverse of the above
	private final short[] decryptArr;
	// same as fourSq, but instead represented as java chars
	private final char[][] sqChars;
	
	/**
	 * Running time: O(1)
	 * Reasoning: Despite the 2 loops, there is no "input" as such, and they
	 * will always run from 0 to 127. As such, this will always run in the same
	 * amount of time (approx.).
	 * 
	 * Space complexity: ~768 bytes
	 * Reasoning: 256 bytes + 2*256 bytes (2 bytes per short)
	 */
	static {
		PACKED_CHARS = new byte[256];
		UNPACKED_CHARS = new short[256];
		
		for (short s = 0; s < 128; ++s) {
			PACKED_CHARS[s] = packChar(s);
		}
		
		for (short s = 0; s < 128; ++s) {
			UNPACKED_CHARS[s] = unpackChar(s);
		}
	}
	
	/**
	 * Running time: O(n^4)
	 * Reasoning: At one point this method pairs every character in a 2d array
	 * with every other character in that array, making it O(n^4) (where 'n' is
	 * SQRT_ALPHABET_SIZE). You could also say it's every permutation of 4
	 * characters, n*n*n*n, n^4.
	 * 
	 * Space complexity: O(n^2)
	 * Reasoning: The lookup tables must hold every permutation of 2 characters
	 * from the alphabet. This is n * n characters, or n*2.
	 */
	public Cipher(String key) {
		encryptArr = new short[SQUARED_ALPHABET_SIZE];
		decryptArr = new short[SQUARED_ALPHABET_SIZE];
		
		// populate top left / bottom right quadrants
		fourSq = new byte[3][SQRT_ALPHABET_SIZE][SQRT_ALPHABET_SIZE];
		final int fullSqSize = 2 * SQRT_ALPHABET_SIZE;
		sqChars = new char[fullSqSize][fullSqSize];
		
		byte charCounter = 0;
		for (int i = 0; i < SQRT_ALPHABET_SIZE; ++i) {
			for (int j = 0; j < SQRT_ALPHABET_SIZE; ++j) {
				byte compactVal = charCounter++;
				fourSq[0][i][j] = charCounter;
				char charVal = (char)UNPACKED_CHARS[compactVal];
				
				sqChars[i][j] = charVal;
				sqChars[i + SQRT_ALPHABET_SIZE][j + SQRT_ALPHABET_SIZE] = charVal;
			}
		}
		
		// populate top right / bottom left quadrants with the encryption key
		int keyIndex = 0;
		for (int i = 1; i <= 2; ++i) {
			for (int j = 0; j < SQRT_ALPHABET_SIZE; ++j) {
				for (int k = 0; k < SQRT_ALPHABET_SIZE; ++k) {
					char keyChar = key.charAt(keyIndex++);
					fourSq[i][j][k] = PACKED_CHARS[(byte) keyChar];
					
					if (i == 1) {
						sqChars[j][k + SQRT_ALPHABET_SIZE] = keyChar;
					}
					else {
						sqChars[j + SQRT_ALPHABET_SIZE][k] = keyChar;
					}
				}
			}
		}
		
		int arrCounter = 0;
		// generate the short arrays used for encryption/decryption
		for (int c1y = 0; c1y < SQRT_ALPHABET_SIZE; ++c1y) {
			for (int c1x = 0; c1x < SQRT_ALPHABET_SIZE; ++c1x) {
				for (int c2y = 0; c2y < SQRT_ALPHABET_SIZE; ++c2y) {
					for (int c2x = 0; c2x < SQRT_ALPHABET_SIZE; ++c2x) {
						short toChar1 = fourSq[1][c1y][c2x];
						short toChar2 = fourSq[2][c2y][c1x];
						
						// use bit shifts to store 2x 6 bit numbers as a single short
						short combined = (short) (toChar1 << 6 | toChar2);
						encryptArr[arrCounter++] = combined;
					}
				}
			}
		}
		
		// decryption array is just the reverse of the encryption array
		// (indexes swapped with values at that index)
		for (short i = 0; i < encryptArr.length; ++i) {
			decryptArr[encryptArr[i]] = i;
		}
	}
	
	/**
	 * Running time: O(n)
	 * Reasoning: Scales with ALPHABET_SIZE (for every character in the alphabet,
	 * it needs to be added and later plucked out at random)
	 * 
	 * Space complexity: O(n)
	 * Reasoning: Same reasoning as above.
	 */
	public static String generateRandomKey() {
		Random random = new Random();
		char[] key = new char[ALPHABET_SIZE * 2];
		int pos = 0;
		int i;
		short j;
		
		List<Character> charPool = new LinkedList<Character>();
		
		for (i = 0; i < 2; ++i) {
			for (j = 0; j < ALPHABET_SIZE; ++j) {
				charPool.add((char)unpackChar(j));
			}
			
			while (!charPool.isEmpty()) {
				key[pos++] = charPool.remove(random.nextInt(charPool.size()));
			}
		}
		
		return new String(key);
	}
	
	/**
	 * NOTE: Functions identically to decryptChars()
	 * 
	 * Running time: O(1)
	 * Reasoning: Exact same process regardless of what a or b are.
	 * Always uses 3 array lookups, each running in constant time.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: (2 + 2) = ~4 bytes
	 */
	public byte[] encryptChars(byte a, byte b) {
		byte[] encrypted = new byte[2];
		short combinedResult = encryptArr[(short) (a << 6 | b)];
		
		encrypted[0] = (byte) UNPACKED_CHARS[combinedResult >> 6];
		encrypted[1] = (byte) UNPACKED_CHARS[combinedResult & 0x3F];
		
		return encrypted;
	}
	
	/**
	 * NOTE: Functions identically to encryptChars()
	 * 
	 * Running time: O(1)
	 * Reasoning: Exact same process regardless of what a or b are.
	 * Always uses 3 array lookups, each running in constant time.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: (2 + 2) = ~4 bytes
	 */
	public byte[] decryptChars(byte a, byte b) {
		byte[] decrypted = new byte[2];
		short combinedResult = decryptArr[(short) (a << 6 | b)];
		
		decrypted[0] = (byte) UNPACKED_CHARS[combinedResult >> 6];
		decrypted[1] = (byte) UNPACKED_CHARS[combinedResult & 0x3F];
		
		return decrypted;
	}
	
	/**
	 * Running time: O(1)
	 * Reasoning: Running time varies depending on what c is, but not in a way
	 * the can be represented in Big O.
	 * 
	 * Space complexity: 4 bytes
	 * Reasoning: 1x int
	 */
	private static byte packChar(short c) {
		int cVal = c;
		
		if (cVal >= 65 && cVal <= 90) {
			return (byte) (cVal - 65);
		} else if (cVal >= 97 && cVal <= 122) {
			return (byte) (cVal - 97 + 26);
		} else if (cVal >= 48 && cVal <= 57) {
			return (byte) (cVal - 48 + (26 * 2));
		}else if (cVal == 32) {
			return 62;
		}else  if (cVal == 44){
			return 63;
		}
		
		// char not to be encrypted
		return -1;
	}
	
	/**
	 * Running time: O(1)
	 * Reasoning: Running time varies depending on what c is, but not in a way
	 * the can be represented in Big O.
	 * 
	 * Space complexity: 4 bytes
	 * Reasoning: 1x int
	 */
	private static short unpackChar(short c) {
		int cVal = c;
		
		if (cVal < 26) {
			return (short) (cVal + 65);
		} else if (cVal >= 26 && cVal < 52) {
			return (short) (cVal - 26 + 97);
		} else if (cVal >= 52 && cVal < 62) {
			return (short) (cVal - 52 + 48);
		}else if (cVal == 62) {
			return 32;
		}else {
			return 44;
		}
	}
	
	/**
	 * Running time: O(n^2), where 'n' is the length of the square
	 * Reasoning: Prints a square of chars, side size n. A squares area
	 * obviously grows by it's length, squared.
	 * 
	 * Space complexity: 0 bytes
	 * Reasoning: No variables held in memory, just prints to the screen.
	 */
	public void printSquares() {
		System.out.println();
		for (int r = 0; r < sqChars.length; ++r) {
			System.out.print(" ");
			for (int c = 0; c < sqChars[r].length; ++c) {
				System.out.print(sqChars[r][c] + " ");
				
				if (c == sqChars[r].length / 2 - 1) {
					System.out.print("| ");
				}
			}
			System.out.println();
			
			if (r == sqChars.length / 2 - 1) {
				for (int i = 0; i < sqChars[r].length + 1; ++i) {
					if (i == sqChars[r].length / 2) {
						System.out.print(" +");
					}
					else {
						System.out.print(" -");
					}
				}
				System.out.println();
			}
		}
		
		System.out.println();
	}
	
	public void processFile(String fileName, boolean encryptMode, boolean readFromURL, boolean writeToFile) {
		CipherProcessor cipherProcessor = new CipherProcessor(fileName, encryptMode, readFromURL, writeToFile);
		cipherProcessor.processFile();
	}
	
	private class CipherProcessor {
		private String resourcePath;
		private boolean encryptMode;
		private boolean writeToFile;
		private boolean readFromURL;
		
		private static final int BUFFER_LEN = 8192;
		private BufferedOutputStream out;
		private int outputCounter;
		private byte[] outputBytes;
		
		public CipherProcessor(String resourcePath, boolean encryptMode, boolean readFromURL, boolean writeToFile) {
			this.encryptMode = encryptMode;
			this.resourcePath = resourcePath;
			this.writeToFile = writeToFile;
			this.readFromURL = readFromURL;
		}
		
		/**
		 * Running time: O(n)
		 * Reasoning: A complex method, but overall since each byte is read,
		 * dealt with, then written, it runs in O(n) time.
		 * 
		 * 
		 * Space complexity: O(1)
		 * Reasoning: Since the file is encrypted "on the fly" and not held in memory
		 * all at once, this method should use approximatly the same amount of space
		 * regardless of file size.
		 */
		public void processFile() {
			long encNs = 0;
			long encStart;
			
			try {
				String inputFileName = new File(resourcePath).getName();
				String fileOutputPath;
				
				InputStream inStream;
				OutputStream outStream;
				URL url;
				
				if (readFromURL) {
					url = new URL(resourcePath);
					inStream = url.openStream();
				}
				else {
					inStream = new FileInputStream(resourcePath);
				}
				
				if (writeToFile) {
					// strip off the file extension, if there is one
					if (inputFileName.contains(".")) {
						inputFileName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
					}
					
					fileOutputPath = String.format("%s/output/%s%s.txt",
							Menu.ROOT_DIR,
							inputFileName,
							(encryptMode ? "_enc" : "_dec"));
					
					outStream = new FileOutputStream(fileOutputPath);
				}
				else {
					outStream = System.out;
				}
				
				BufferedInputStream in = new BufferedInputStream(inStream);
				out = new BufferedOutputStream(outStream);
				
				byte[] inputBytes = new byte[BUFFER_LEN];
				int bytesRead;
				byte bigramA = 0, bigramB;
				boolean charStored = false;
				byte b;
				byte packedByte;
				byte[] processedBytes = new byte[2];
				// initial size for the carriedChars ArrayList
				final int CARRIED_CHARS_INITIAL_SIZE = 5;
				List<Byte> carriedChars = null;
				
				outputBytes = new byte[BUFFER_LEN];
				outputCounter = 0;
				
				while ((bytesRead = in.read(inputBytes)) != -1) {
					encStart = System.nanoTime();
					
					for (int i = 0; i < bytesRead; ++i) {
						b = inputBytes[i];
						
						if (b < 0) {
							// non ASCII character
							packedByte = -1;
						}
						else {
							// ASCII character
							packedByte = PACKED_CHARS[b];
						}
						
						if (packedByte != -1) {
							// byte should be encrypted
							if (charStored) {
								bigramB = packedByte;
								
								if (encryptMode) {
									processedBytes = encryptChars(bigramA, bigramB);	
								}
								else {
									processedBytes = decryptChars(bigramA, bigramB);
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
						
						if (outputCounter == BUFFER_LEN) {
							encNs += System.nanoTime() - encStart;
							writeIfFull();
							encStart = System.nanoTime();
						}
					}
					encNs += System.nanoTime() - encStart;
				}
				
				double encTakenMs = encNs / 1000000d;
				
				System.out.printf("\n\nTime spent encrypting/decrypting only: %.2fms\n", encTakenMs);
				
				in.close();
				
				//System.out.println("Output counter: " + outputCounter);
				out.write(outputBytes, 0, outputCounter);
				if (charStored) {
					bigramB = PACKED_CHARS[(short) ' '];
					
					if (encryptMode) {
						processedBytes = encryptChars(bigramA, bigramB);	
					}
					else {
						processedBytes = decryptChars(bigramA, bigramB);
					}
					
					out.write(processedBytes[0]);
					
					if (carriedChars != null) {
						for (int i = 0; i < carriedChars.size(); ++i) {
							out.write(carriedChars.get(i));
						}
						
						carriedChars = null;
					}
					
					out.write(processedBytes[1]);
				}
				
				if (carriedChars != null) {
					System.out.println("Carried chars size: " + carriedChars.size());
					for (int i = 0; i < carriedChars.size(); ++i) {
						out.write(carriedChars.get(i));
					}
				}
				
				if (writeToFile) {
					out.close();
				}
				else {
					System.out.print("\n\n");
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Running time: O(n)
		 * Reasoning: Because of the call to writeIfFull(), worst case this has to write
		 * n bytes.
		 * 
		 * Space complexity: 0
		 * Reasoning: No extra variables.
		 */
		private void writeBytes(byte[] processedBytes) throws IOException {
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
		
		/**
		 * Running time: O(n)
		 * Reasoning: Because of the call to writeIfFull(), worst case this has to write
		 * n bytes.
		 * 
		 * Space complexity: 0
		 * Reasoning: No extra variables.
		 */
		private void writeBytes(byte[] processedBytes, List<Byte> carriedChars) throws IOException {
			outputBytes[outputCounter++] = processedBytes[0];
			
			writeIfFull();
			
			for (int j = 0; j < carriedChars.size(); ++j) {
				outputBytes[outputCounter++] = carriedChars.get(j);
				writeIfFull();
			}
			
			outputBytes[outputCounter++] = processedBytes[1];
		}
		
		/**
		 * Running time: O(n)
		 * Reasoning: Running times grows linearly depending on BUFFER_LEN.
		 * 
		 * Space complexity: 0
		 * Reasoning: No extra variables.
		 */
		private void writeIfFull() throws IOException {
			if (outputCounter == BUFFER_LEN) {
				out.write(outputBytes);
				//arw.writeBytes(outputBytes);
				outputCounter = 0;
			}
		}
	}
	
}
