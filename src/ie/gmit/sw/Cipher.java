package ie.gmit.sw;

import static java.lang.System.out;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Everything to do with the cipher. Holds information about the alphabet
 * being used, the key, the lookup tables for encryption/decryption, etc.
 * 
 * Also includes various methods including encrypting/decrypting batches of
 * characters at once.
 */
public final class Cipher {
	// The alphabet. Any characters here found in the input will be put into a
	// bigram and encrypted.
	public static final String ALPHABET_STRING
		= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ,.!?'\"/-=_+*~();:\n";
	
	// Size of the alphabet. Must be a square number.
	public static final int ALPHABET_SIZE = ALPHABET_STRING.length();
	// squared/square root of alphabet size for calculating array sizes and looping
	public static final int SQUARED_ALPHABET_SIZE = (int)Math.pow(ALPHABET_SIZE, 2);
	public static final int SQRT_ALPHABET_SIZE = (int)Math.sqrt(ALPHABET_SIZE);
	// number of bits needed to fit a "packed" char.
	// should be ceil(log2(ALPBHABET_SIZE))
	private static final byte packedBits = 7;
	// maximum value for packedBits bits
	private static final byte packedBitsMax = 0xFF >> (8 - packedBits);
	
	// any unsupported characters will be replaced by this character
	public static final byte UNKNOWN_PLACEHOLDER = '?';
	public static final byte UNKNOWN_PLACEHOLDER_PACKED;
	
	// The key for the cipher. Should be (2 * ALPHABET_SIZE) characters long
	private final String key;
	// 3d array (3x 2 dim), containing the characters in the four squares,
	// in "packed" form.
	private final byte[][][] fourSq;
	// a lookup table to convert a java char to a "packed" char:
	// a packed char is packedBits bits, with 0 being A, B being 1, all the
	// way up to 80 -> \n
	public static final byte[] PACKED_CHARS;
	// the reverse of the above (converts packed chars back to java chars)
	public static final byte[] UNPACKED_CHARS;
	// Another lookup table for encryption. Usage explained in the README.txt
	private final short[] encryptArr;
	// simply the reverse of above; indexes swapped with values
	private final short[] decryptArr;
	// same as fourSq, but instead represented as java chars
	private final char[][] sqChars;
	
	// the object used to process files/URLs
	private CipherProcessor cipherProcessor;
	
	/**
	 * Running time: O(n)
	 * Reasoning: Runtime proportional to ALPHABET_SIZE.
	 * 
	 * Space complexity: ~209 bytes
	 * Reasoning: 128 + 81 bytes; for the pack and unpack arrays.
	 * 
	 * 
	 * 
	 * Initialises the lookup tables to convert a normal Java char to a "packed char",
	 * and back again.
	 */
	static {
		PACKED_CHARS = new byte[128];
		UNPACKED_CHARS = new byte[ALPHABET_SIZE];
		
		short i;
		
		// get the packed version of the UNKNOWN_PLACEHOLDER
		UNKNOWN_PLACEHOLDER_PACKED = (byte)ALPHABET_STRING.indexOf(UNKNOWN_PLACEHOLDER);
		
		// Initialise the packed arrays
		// In this way, any unsupported ASCII characters
		// are automatically mapped to the UNKNOWN_PLACEHOLDER;
		// a question mark, and will process the same as valid chars.
		for (i = 0; i < PACKED_CHARS.length; ++i) {
			PACKED_CHARS[i] = UNKNOWN_PLACEHOLDER_PACKED;
		}
		
		// generate the packed char lookup table
		for (i = 0; i < ALPHABET_SIZE; ++i) {
			PACKED_CHARS[ALPHABET_STRING.charAt(i)] = (byte)i;
		}
		
		// generate the unpacked char lookup table
		for (i = 0; i < ALPHABET_SIZE; ++i) {
			UNPACKED_CHARS[i] = (byte)ALPHABET_STRING.charAt(i);
		}
		
		// read carriage return as a space (it should be ignored)
		// the sample input files don't have these anyway
		PACKED_CHARS['\r'] = (byte)ALPHABET_STRING.indexOf(' ');
	}
	
	/**
	 * (Not including method calls for the Big O)
	 * 
	 * Running time: O(1)
	 * Reasoning: Just initialising variables.
	 * 
	 * Space complexity: O(n^2)
	 * Reasoning: The lookup tables must hold every permutation of 2 characters
	 * from the alphabet. This is n * n characters, or n^2.
	 */
	public Cipher(String key) {
		this.key = key;
		
		// must initialise these arrays here instead of in init() as they are marked final
		final int fullSqSize = 2 * SQRT_ALPHABET_SIZE;
		
		// lookup tables need to house every permutation of 2x numbers of max size ALPHABET_SIZE
		final int ENCDEC_TABLE_SIZE = ((ALPHABET_SIZE - 1) << packedBits | ALPHABET_SIZE);
		encryptArr = new short[ENCDEC_TABLE_SIZE];
		decryptArr = new short[ENCDEC_TABLE_SIZE];
		
		fourSq = new byte[3][SQRT_ALPHABET_SIZE][SQRT_ALPHABET_SIZE];
		sqChars = new char[fullSqSize][fullSqSize];
		
		cipherProcessor = new CipherProcessor(this);
		
		init(key);
	}
	
	/**
	 * Running time: O(n^4)
	 * Reasoning: At one point this method pairs every character in a 2d array
	 * with every other character in that array, making it O(n^4) (where 'n' is
	 * SQRT_ALPHABET_SIZE). You could also say it's every permutation of 4
	 * characters, n*n*n*n = n^4.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Some extra variables.
	 */
	private void init(String key) {
		int keyIndex = 0;
		int inputBigram = 0;
		int c1x, c2x;
		int c1y, c2y;
		int i, j, k;
		
		// populate the alphabet quadrant
		// (only needs to be done once; having two would be a waste)
		byte compactVal = 0;
		for (i = 0; i < SQRT_ALPHABET_SIZE; ++i) {
			for (j = 0; j < SQRT_ALPHABET_SIZE; ++j) {
				// 3d array used to initialise the lookup tables
				fourSq[0][i][j] = compactVal;
				char charVal = (char)UNPACKED_CHARS[compactVal];
				
				// 2d array used to display the four squares to the user
				sqChars[i][j] = charVal;
				sqChars[i + SQRT_ALPHABET_SIZE][j + SQRT_ALPHABET_SIZE] = charVal;
				
				++compactVal;
			}
		}
		
		// populate top right / bottom left quadrants with the encryption key
		for (i = 1; i <= 2; ++i) {
			for (j = 0; j < SQRT_ALPHABET_SIZE; ++j) {
				for (k = 0; k < SQRT_ALPHABET_SIZE; ++k) {
					char keyChar = key.charAt(keyIndex++);
					// put the packed version of the key char in the square
					fourSq[i][j][k] = PACKED_CHARS[(byte) keyChar];
					
					if (i == 1) {
						// first key; put it in the top right of the square
						sqChars[j][k + SQRT_ALPHABET_SIZE] = keyChar;
					}
					else {
						// first key; put it in the bottom left of the square
						sqChars[j + SQRT_ALPHABET_SIZE][k] = keyChar;
					}
				}
			}
		}
		
		// make sure any unused indexes won't be used
		for (i = 0; i < encryptArr.length; ++i) {
			encryptArr[i] = -1;
		}
		
		// generate the encryption lookup table
		// Note: counting up conveniently cycles through all permutations
		// of bigrams when we represent them as 1 number.
		// (We do have to jump a few indexes sometimes though,
		// since from 81 to 127 is unused)
		for (c1y = 0; c1y < SQRT_ALPHABET_SIZE; ++c1y) {
			for (c1x = 0; c1x < SQRT_ALPHABET_SIZE; ++c1x) {
				for (c2y = 0; c2y < SQRT_ALPHABET_SIZE; ++c2y) {
					for (c2x = 0; c2x < SQRT_ALPHABET_SIZE; ++c2x) {
						// the two characters we're generating the encrypted lookup for
						// (all of this is in packed form)
						
						// This is the "easy" and slow way of encrypting; 1st char is the row
						// of 1 but column of 2 in the key square, etc. But this method of
						// encryption is only done once per bigram permutation, in the
						// Initialisation here. After that, the resulting lookup tables
						// are used.
						short toChar1 = fourSq[1][c1y][c2x];
						short toChar2 = fourSq[2][c2y][c1x];
						
						// use bit shifts to store the 2 characters in 1 short
						short combined = (short)(toChar1 << packedBits | toChar2);
						
						// Set the value. This  way, later when we look up a combined
						// bigram short in this array, we get the resulting encrypted
						// bigram. So the encryption itself is actually performed in
						// a single array lookup.
						encryptArr[inputBigram] = combined;
						
						// move on to the next bigram permutation
						++inputBigram;
						if ((inputBigram & packedBitsMax) == ALPHABET_SIZE) {
							// these next few indexes are unused; skip to the next used one
							inputBigram = ((inputBigram >> packedBits) + 1) << packedBits;
						}
					}
				}
			}
		}
		
		// decryption array is just the reverse of the encryption array
		// (indexes swapped with values at that index)
		for (short s = 0; s < encryptArr.length; ++s) {
			// skip any unused indexes
			if (encryptArr[s] != -1) {
				decryptArr[encryptArr[s]] = s;
			}
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
	 * Running time: O(n)
	 * Reasoning: A single bigram is encrypted in O(1) time (ie. just bit shifts,
	 * array accesses, if statements etc.) but since we're encrypting an array of n
	 * bytes, the running time will scale O(n).
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Conversions are done in place, so the only extra memory taken up
	 * is for a few variables.
	 * 
	 * @param buffer The array of bytes to encrypt
	 * @param limit How much of the array to encrypt
	 */
	public void encryptAll(byte[] buffer, int limit) {
		int i;
		short combinedResult;
		
		// go through the buffer, in steps of 2
		for (i = 0; i < limit; i += 2) {
			try {
				// use bit shifts to store 2 characters in 1 short, then use the lookup table
				combinedResult = encryptArr[PACKED_CHARS[buffer[i]] << packedBits | PACKED_CHARS[buffer[i + 1]]];
			} catch(ArrayIndexOutOfBoundsException e) {
				// Bigram contains a non-ASCII character.
				// Using a try catch like this is faster than fixing it beforehand
				// with if statements for ASCII characters, but slower for non-ASCII
				// characters.
				
				// Files with a large amount of non-ASCII characters (ie. large Irish text files)
				// probably shouldn't be used anyway since all those characters would be replaced
				// by question marks.
				
				if (buffer[i] >> 7 == -1) {
					// non-ASCII; convert to UNKNOWN_PLACEHOLDER
					buffer[i] = UNKNOWN_PLACEHOLDER;
				}
				
				if (buffer[i + 1] >> 7 == -1) {
					// non-ASCII; convert to UNKNOWN_PLACEHOLDER
					buffer[i + 1] = UNKNOWN_PLACEHOLDER;
				}
				
				// now that the non-ASCII characters are replaced with placeholders, encrypt again
				combinedResult = encryptArr[PACKED_CHARS[buffer[i]] << packedBits | PACKED_CHARS[buffer[i + 1]]];
			}
			
			// use bit shifts to get the 2 characters back out of the single short value
			buffer[i] = UNPACKED_CHARS[(combinedResult >> packedBits)];
			buffer[i + 1] = UNPACKED_CHARS[(combinedResult & packedBitsMax)];
		}
	}
	
	/**
	 * (same as above)
	 * 
	 * Running time: O(n)
	 * Reasoning: A single bigram is decrypted in O(1) time (ie. just bit shifts,
	 * array accesses, if statements etc.) but since we're decrypting an array of n
	 * bytes, the running time will scale O(n).
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Conversions are done in place, so the only extra memory taken up
	 * is for a few variables.
	 * 
	 * @param buffer The array of bytes to encrypt
	 * @param limit How much of the array to encrypt
	 */
	public void decryptAll(byte[] buffer, int limit) {
		int i;
		short combinedResult;
		
		for (i = 0; i < limit; i += 2) {
			// Note: No need for try/catch here as in the encryptAll method, as we know
			// encrypted bytes will be ASCII; any non-ASCII characters in the input before
			// encrypting were thrown out.
			
			// use bit shifts to store 2 characters in 1 short, then use the lookup table
			combinedResult = decryptArr[PACKED_CHARS[buffer[i]] << packedBits | PACKED_CHARS[buffer[i + 1]]];
			
			// use bit shifts to get the 2 characters back out of the single short value
			buffer[i] = UNPACKED_CHARS[(combinedResult >> packedBits)];
			buffer[i + 1] = UNPACKED_CHARS[(combinedResult & packedBitsMax)];
		}
	}
	
	/**
	 * Running time: O(n)
	 * Reasoning: n characters to be printed.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Some extra variables.
	 */
	public void printKey() {
		int i;
		char c;
		
		out.println("Key:");
		
		for (i = 0; i < key.length(); ++i) {
			c = key.charAt(i);
			
			// don't print newlines as newlines
			// should use a placeholder character, '^'
			if (c == '\n') {
				out.print("^");
			}
			else {
				out.print(c);
			}
		}
		
		out.print("\n\n");
	}
	
	/**
	 * Running time: O(n^2), where 'n' is the length of the square
	 * Reasoning: Prints a square of chars, side size n. A squares area
	 * obviously grows by it's length, squared.
	 * 
	 * Space complexity: O(1)
	 * Reasoning: Some extra variables.
	 * 
	 * 
	 * 
	 * Prints the 4 squares from the cipher in ASCII form.
	 * Top left/bottom right squares are the alphabet, top right
	 * and bottom left squares are the two key parts, as in the PDF.
	 */
	public void printSquares() {
		int i, r, c;
		char sqc;
		
		for (r = 0; r < sqChars.length; ++r) {
			out.print(" ");
			// print the alphabet/key characters
			for (c = 0; c < sqChars[r].length; ++c) {
				sqc = sqChars[r][c];
				// don't print newlines as newlines
				// should use a placeholder character, '^'
				if (sqc == '\n') {
					out.print("^ ");
				}
				else {
					out.print(sqChars[r][c] + " ");
				}
				
				// print the middle line going down
				if (c == sqChars[r].length / 2 - 1) {
					out.print("| ");
				}
			}
			out.println();
			
			if (r == sqChars.length / 2 - 1) {
				// print the middle line going across
				for (i = 0; i < (sqChars[r].length + 1) * 2; ++i) {
					if (i == sqChars[r].length + 1) {
						out.print("+");
					}
					else {
						out.print("-");
					}
				}
				out.println();
			}
		}
	}
	
	/**
	 * Just calls another object's; see that method for Big O details.
	 */
	public void processFile(String fileName,
			boolean encryptMode,
			boolean readFromURL,
			boolean writeToFile)
				throws IOException {
		cipherProcessor.processFile(fileName, encryptMode, readFromURL, writeToFile);
	}
	
}
