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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class Cipher {
	// number of characters which can be encrypted,
	// i.e. number of characters in 1 quadrant of the four squares
	public static final String ALPHABET_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ,";
	public static final int ALPHABET_SIZE = ALPHABET_STRING.length();
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
	public static final byte[] UNPACKED_CHARS;
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
	 * 
	 * 
	 * 
	 * Initialises the lookup tables to convert a normal Java char to a "packed char";
	 * simply A = 0, B = 1, all the way up to ',' = 63.
	 * 
	 * This conversion is already done in packChar() and unpackChar().
	 */
	static {
		PACKED_CHARS = new byte[256];
		UNPACKED_CHARS = new byte[256];
		
		byte i;
		
		for (i = 0; i < Byte.MAX_VALUE; ++i) {
			PACKED_CHARS[i] = -1;
			UNPACKED_CHARS[i] = -1;
		}
		
		for (i = 0; i < ALPHABET_SIZE; ++i) {
			PACKED_CHARS[ALPHABET_STRING.charAt(i)] = i;
		}
		
		for (i = 0; i < ALPHABET_SIZE; ++i) {
			UNPACKED_CHARS[i] = (byte) ALPHABET_STRING.charAt(i);
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
		// must initialise these arrays here instead of in init() as they are marked final
		final int fullSqSize = 2 * SQRT_ALPHABET_SIZE;
		
		encryptArr = new short[SQUARED_ALPHABET_SIZE];
		decryptArr = new short[SQUARED_ALPHABET_SIZE];
		
		fourSq = new byte[3][SQRT_ALPHABET_SIZE][SQRT_ALPHABET_SIZE];
		
		sqChars = new char[fullSqSize][fullSqSize];
		
		init(key);
	}
	
	private void init(String key) {
		// populate the alphabet quadrant
		// (only needs to be done once; having two would be a waste)
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
		// generate the lookup tables (short arrays) used for encryption/decryption
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
		// (indexesswapped with values at that index)
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
			// add all the chars from the alphabet into a linkedlist pool of chars
			for (j = 0; j < ALPHABET_SIZE; ++j) {
				charPool.add((char)UNPACKED_CHARS[j]);
			}
			
			// pluck them out at random to generate a random key
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
		// use bit shifts to store 2x 6 bit numbers as a single short, then use the lookup table
		short combinedResult = encryptArr[a << 6 | b];
		
		// separate the 6 bit number back out, again with bit shifts
		encrypted[0] = UNPACKED_CHARS[combinedResult >> 6];
		encrypted[1] = UNPACKED_CHARS[combinedResult & 0x3F];
		
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
		// use bit shifts to store 2x 6 bit numbers as a single short, then use the lookup table
		short combinedResult = decryptArr[a << 6 | b];
		
		// separate the 6 bit number back out, again with bit shifts
		decrypted[0] = UNPACKED_CHARS[combinedResult >> 6];
		decrypted[1] = UNPACKED_CHARS[combinedResult & 0x3F];
		
		return decrypted;
	}
	
	/**
	 * Running time: O(n^2), where 'n' is the length of the square
	 * Reasoning: Prints a square of chars, side size n. A squares area
	 * obviously grows by it's length, squared.
	 * 
	 * Space complexity: 0 bytes
	 * Reasoning: No variables held in memory, just prints to the screen.
	 * 
	 * Prints the 4 squares from the cipher in ASCII form.
	 * Top left/bottom right squares are the alphabet, top right
	 * and bottom left squares are the two key parts, as in the PDF.
	 */
	public void printSquares() {
		System.out.println();
		for (int r = 0; r < sqChars.length; ++r) {
			System.out.print(" ");
			// print the alphabet/key characters
			for (int c = 0; c < sqChars[r].length; ++c) {
				System.out.print(sqChars[r][c] + " ");
				
				// print the middle line going down
				if (c == sqChars[r].length / 2 - 1) {
					System.out.print("| ");
				}
			}
			System.out.println();
			
			if (r == sqChars.length / 2 - 1) {
				// print the middle line going accross
				for (int i = 0; i < (sqChars[r].length + 1) * 2; ++i) {
					if (i == sqChars[r].length + 1) {
						System.out.print("+");
					}
					else {
						System.out.print("-");
					}
				}
				System.out.println();
			}
		}
		
		System.out.println();
	}
	
	public void processFile(String fileName, boolean encryptMode, boolean readFromURL, boolean writeToFile) throws IOException {
		CipherProcessor cipherProcessor = new CipherProcessor(fileName, encryptMode, readFromURL, writeToFile);
		cipherProcessor.processFile();
	}
	
	/**
	 * Handles reading from file/url and writing to a file/console,
	 * feeding all bytes through the parent Cipher object.
	 */
	private class CipherProcessor {
		// settings
		private String resourcePath;
		private boolean encryptMode;
		// Encrypt Mode
		// True: Encrypt
		// False: Decrypt
		private boolean writeToFile;
		// Write to File
		// True: Write to File
		// False: Write to Standard Out (the console)
		private boolean readFromURL;
		// Read from URL
		// True: Read from URL
		// False: Read from File
		
		// how many bytes to be read into the buffer each time
		// the value of 8192 seems most efficient
		private static final int BUFFER_LEN = 8192;
		private BufferedOutputStream out;
		// keeps count of the amount of bytes written to the buffer
		private int outputCounter;
		// the output buffer
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
		 * all at once, this method should use approximately the same amount of space
		 * regardless of file size.
		 */
		public void processFile() throws IOException {
			// the input byte buffer
			byte[] inputBytes = new byte[BUFFER_LEN];
			// keeps track of what the last byte read from the buffer was
			int bytesRead;
			// used to store a single bigram, as "packed" values
			byte bigramA = 0, bigramB;
			// used to keep track if bigramA is filled, so that unsupported characters
			// can be written in the right order, and to know where to store the next char
			// (in bigramA or bigramB)
			boolean charStored = false;
			// byte read in
			byte b;
			// "packed" form of b. See Cipher.PACKED_CHARS
			byte packedByte;
			// encrypted/decrypted of bigramA and bigramB
			byte[] processedBytes = new byte[2];
			// initial size for the carriedChars ArrayList
			final int CARRIED_CHARS_INITIAL_SIZE = 5;
			// unsupported character buffer
			List<Byte> carriedChars = null;
			
			long encNs = 0;
			long encStart;
			
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
				
				// read from file
				inStream = new FileInputStream(resourcePath);
			}
			
			if (writeToFile) {
				// strip off the file extension, if there is one
				if (inputFileName.contains(".")) {
					inputFileName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
				}
				
				// form the output path string
				fileOutputPath = String.format("%s/output/%s%s.txt",
						Menu.ROOT_DIR,
						inputFileName,
						(encryptMode ? "_enc" : "_dec"));
				
				outStream = new FileOutputStream(fileOutputPath);
			}
			else {
				// write to standard out
				outStream = System.out;
			}
			
			BufferedInputStream in = new BufferedInputStream(inStream);
			out = new BufferedOutputStream(outStream);
			
			outputBytes = new byte[BUFFER_LEN];
			outputCounter = 0;
			
			// fill the buffer until no more bytes are available
			while ((bytesRead = in.read(inputBytes)) != -1) {
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
							// bigramA is filled, bigramB is empty
							bigramB = packedByte;
							
							if (encryptMode) {
								// encrypt
								processedBytes = encryptChars(bigramA, bigramB);	
							}
							else {
								// decrypt
								processedBytes = decryptChars(bigramA, bigramB);
							}
							
							// make sure to output any unsupported characters found between bigramA and bigramB
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
							// bigramA and bigramB both empty
							bigramA = packedByte;
							charStored = true;
						}
					}
					else {
						// packedByte == -1
						if (charStored) {
							// bigramA is filled and an unsupported character was found.
							// We must store it until later when bigramB is filled. Then, the
							// bigram can be encrypted/decrypted, then bigramA can be outputted,
							// then the buffer of unsupported characters, then bigramB. This way,
							// the characters will always retain their original order.
							if (carriedChars == null) {
								// start a buffer of unsupported chars, to be written later
								carriedChars = new ArrayList<Byte>(CARRIED_CHARS_INITIAL_SIZE);
							}
							
							// add unsupported character to the buffer
							carriedChars.add(b);
						}
						else {
							// since bigramA is empty this unsupported character can be written right away
							outputBytes[outputCounter++] = b;
						}
					}
					
					if (outputCounter == BUFFER_LEN) {
						// buffer is full; write it and start a new buffer
						writeIfFull();
					}
				}
			}
			
			in.close();
			
			// write the remaining bytes (partially filled buffer)
			out.write(outputBytes, 0, outputCounter);
			if (charStored) {
				// bigramA is full but there's no more chars; add the padding char
				bigramB = PACKED_CHARS[(short) ' '];
				
				if (encryptMode) {
					processedBytes = encryptChars(bigramA, bigramB);	
				}
				else {
					processedBytes = decryptChars(bigramA, bigramB);
				}
				
				out.write(processedBytes[0]);
				
				// write any unsupported chars
				if (carriedChars != null) {
					for (int i = 0; i < carriedChars.size(); ++i) {
						out.write(carriedChars.get(i));
					}
					
					carriedChars = null;
				}
				
				out.write(processedBytes[1]);
			}
			
			// close files etc.
			if (writeToFile) {
				out.close();
			}
			else {
				out.flush();
				System.out.print("\n\n");
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
				// reached end of buffer. must write 1 byte, make a new buffer, then write the other
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
		 * Space complexity: 4 bytes
		 * Reasoning: 1x int var.
		 */
		private void writeBytes(byte[] processedBytes, List<Byte> carriedChars) throws IOException {
			int j;
			
			outputBytes[outputCounter++] = processedBytes[0];
			
			writeIfFull();
			
			for (j = 0; j < carriedChars.size(); ++j) {
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
				outputCounter = 0;
			}
		}
	}
	
}
