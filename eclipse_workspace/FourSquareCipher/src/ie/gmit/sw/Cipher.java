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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class Cipher {
	// number of characters which can be encrypted,
	// i.e. number of characters in 1 quadrant of the four squares
	public static final String ALPHABET_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ,.!?'\"\n/\\-=_+{}();";
	public static final int ALPHABET_SIZE = ALPHABET_STRING.length();
	public static final int SQUARED_ALPHABET_SIZE = (int)Math.pow(ALPHABET_SIZE, 2);
	public static final int SQRT_ALPHABET_SIZE = (int)Math.sqrt(ALPHABET_SIZE);
	// number of bits needed to fit a "packed" char.
	// should be ceil(log2(ALPBHABET_SIZE))
	private static final byte packedBits = 7;
	
	private static final byte qMarkIndex = (byte)ALPHABET_STRING.indexOf('?'); 
	
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
		PACKED_CHARS = new byte[128];
		UNPACKED_CHARS = new byte[ALPHABET_SIZE];
		
		short i;
		
		for (i = 0; i < PACKED_CHARS.length; ++i) {
			PACKED_CHARS[i] = qMarkIndex;
		}
		
		for (i = 0; i < ALPHABET_SIZE; ++i) {
			PACKED_CHARS[ALPHABET_STRING.charAt(i)] = (byte) i;
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
		
		encryptArr = new short[(0x7F << packedBits | 0x7F) + 1];
		decryptArr = new short[(0x7F << packedBits | 0x7F) + 1];
		
		// ensure all unused encryptArr indexes are -1 so they aren't used
		for (int i = 0; i < encryptArr.length; ++i) {
			encryptArr[i] = -1;
		}
		
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
						short combined = (short) (toChar1 << packedBits | toChar2);
						encryptArr[arrCounter] = combined;
						
						arrCounter++;
						if ((arrCounter & 0x7F) == ALPHABET_SIZE) {
							arrCounter = ((arrCounter >> packedBits) + 1) << packedBits;
						}
					}
				}
			}
		}
		
		System.out.println();
		// decryption array is just the reverse of the encryption array
		// (indexes swapped with values at that index)
		for (short i = 0; i < encryptArr.length; ++i) {
			if (encryptArr[i] == -1) {
				// unused index
				continue;
			}
			
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
	public void encryptBigram(byte[] array, int index) {
		// use bit shifts to store 2x 6 bit numbers as a single short, then use the lookup table
		short combinedResult = encryptArr[array[index] << packedBits | array[index + 1]];
		
		// separate the 6 bit number back out, again with bit shifts
		array[index] = UNPACKED_CHARS[combinedResult >> packedBits];
		array[index + 1] = UNPACKED_CHARS[combinedResult & 0x7F];
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
	public void decryptBigram(byte[] array, int index) {
		// use bit shifts to store 2x 6 bit numbers as a single short, then use the lookup table
		short combinedResult = decryptArr[array[index] << packedBits | array[index + 1]];
		
		// separate the 6 bit number back out, again with bit shifts
		array[index] = UNPACKED_CHARS[combinedResult >> packedBits];
		array[index + 1] = UNPACKED_CHARS[combinedResult & 0x7F];
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
				char sqc = sqChars[r][c];
				if (sqc == '\n') {
					System.out.print("nl");
				}
				else {
					System.out.print(sqChars[r][c] + " ");
				}
				
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
			byte[] bufferBytes = new byte[BUFFER_LEN];
			// keeps track of what the last byte read from the buffer was
			int bytesRead;
			
			String inputFileName = new File(resourcePath).getName();
			String fileOutputPath;
			
			InputStream inStream;
			OutputStream outStream;
			URL url;
			
			int i;
			
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
			
			// fill the buffer until no more bytes are available
			while ((bytesRead = in.read(bufferBytes)) != -1) {
				// convert all bytes in the buffer to "packed form"
				for (i = 0; i < bytesRead; ++i) {
					if (bufferBytes[i] < 0) {
						// non ASCII character
						bufferBytes[i] = qMarkIndex;
					}
					else {
						bufferBytes[i] = PACKED_CHARS[bufferBytes[i]];
					}
				}
				
				// encrypt/decrypt byte pairs in place
				for (i = 0; i < bytesRead; i += 2) {
					if (encryptMode) {
						encryptBigram(bufferBytes, i);
					}
					else {
						decryptBigram(bufferBytes, i);
					}
				}
				
				out.write(bufferBytes, 0, bytesRead);
			}
			
			// close files etc.
			in.close();
			if (writeToFile) {
				out.close();
			}
			else {
				out.flush();
				System.out.print("\n\n");
			}
		}
		
	}
	
}
