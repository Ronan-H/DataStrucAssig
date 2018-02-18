package ie.gmit.sw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Cipher {
	public static final int ALPHABET_SIZE = 64;
	public static final int SQUARED_ALPHABET_SIZE = (int)Math.pow(ALPHABET_SIZE, 2);
	public static final int SQRT_ALPHABET_SIZE = (int)Math.sqrt(ALPHABET_SIZE);
	
	private final short[][][] fourSq;
	public static final byte[] PACKED_CHARS;
	public static final short[] UNPACKED_CHARS;
	private final short[] encryptArr;
	private final short[] decryptArr;
	private final char[][] sqChars;
	
	static {
		PACKED_CHARS = new byte[256];
		UNPACKED_CHARS = new short[256];
		
		for (short s = 0; s <= 127; ++s) {
			PACKED_CHARS[s] = packChar(s);
		}
		
		for (short s = 0; s < 128; ++s) {
			UNPACKED_CHARS[s] = unpackChar(s);
		}
	}
	
	public Cipher(String key) {
		encryptArr = new short[SQUARED_ALPHABET_SIZE];
		decryptArr = new short[SQUARED_ALPHABET_SIZE];
		
		// populate top left / bottom right quadrants
		fourSq = new short[3][SQRT_ALPHABET_SIZE][SQRT_ALPHABET_SIZE];
		final int fullSqSize = 2 * SQRT_ALPHABET_SIZE;
		sqChars = new char[fullSqSize][fullSqSize];
		
		short charCounter = 0;
		for (int i = 0; i < SQRT_ALPHABET_SIZE; ++i) {
			for (int j = 0; j < SQRT_ALPHABET_SIZE; ++j) {
				short compactVal = charCounter++;
				fourSq[0][i][j] = (short)charCounter;
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
					fourSq[i][j][k] = PACKED_CHARS[(short) keyChar];
					
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
		
		for (short i = 0; i < encryptArr.length; ++i) {
			decryptArr[encryptArr[i]] = i;
		}
	}
	
	public static String generateKey() {
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
	 * Adjusts a string of characters to work as a key for the cipher.
	 * @param key
	 * @return
	 */
	public static String sanitizeKeys(StringBuilder[] inputKeys) {
		int i, j, k;
		char c = 0;
		int packedChar;
		StringBuilder inputKey;
		String outputKey;
		
		if (inputKeys[1] == null) inputKeys[1] = new StringBuilder(ALPHABET_SIZE);
		
		// Step 1: Remove any characters not in the cipher's alphabet
		for (i = 0; i < 2; ++i) {
			for (j = 0; j < inputKeys[i].length(); ++j) {
				c = inputKeys[i].charAt(i);
				packedChar = (c < 0 ? -1 : PACKED_CHARS[(int) c]);
				
				if (packedChar == -1) {
					// unsupported character
					inputKeys[i].deleteCharAt(j);
					--j;
				}
			}
		}
		
		// Step 2: Remove duplicate characters
		for (i = 0; i < 2; ++i) {
			inputKey = inputKeys[i];
			for (j = 0; j < inputKey.length(); ++j) {
				for (k = 0; k < inputKey.length(); ++k) {
					if (k == j) continue;
					
					if (inputKey.charAt(k) == inputKey.charAt(j)) {
						// duplicate character found
						inputKey.deleteCharAt(k);
						--k;
					}
				}
			}
		}
		
		// Step 3: Append characters, if necessary, to get the right key length
		for (i = 0; i < 2; ++i) {
			inputKey = inputKeys[i];
			outerLoop:
			for (j = 0; j < UNPACKED_CHARS.length && inputKey.length() < ALPHABET_SIZE; ++j) {
				c = (char)UNPACKED_CHARS[j];
				for (k = 0; k < inputKey.length(); ++k) {
					if (inputKey.charAt(k) == c) continue outerLoop;
				}
				
				// character not found; append it to the key
				inputKey.append(c);
			}
		}
		
		outputKey = inputKeys[0].append(inputKeys[1]).toString();
		return outputKey;
	}
	
	public byte[] encryptChars(byte a, byte b) {
		byte[] encrypted = new byte[2];
		short combinedResult = encryptArr[(short) (a << 6 | b)];
		
		encrypted[0] = (byte) UNPACKED_CHARS[combinedResult >> 6];
		encrypted[1] = (byte) UNPACKED_CHARS[combinedResult & 0x3F];
		
		return encrypted;
	}
	
	public byte[] decryptChars(byte a, byte b) {
		byte[] decrypted = new byte[2];
		short combinedResult = decryptArr[(short) (a << 6 | b)];
		
		decrypted[0] = (byte) UNPACKED_CHARS[combinedResult >> 6];
		decrypted[1] = (byte) UNPACKED_CHARS[combinedResult & 0x3F];
		
		return decrypted;
	}
	
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
					System.out.print("- ");
				}
				System.out.println();
			}
		}
		
		System.out.println();
	}
	
	public void processFile(String fileName, boolean encryptMode, boolean outputToFile) {
		CipherProcessor cipherProcessor = new CipherProcessor(fileName, encryptMode, outputToFile);
		cipherProcessor.processFile();
	}
	
	private class CipherProcessor {
		private String fileName;
		private boolean encryptMode;
		private boolean outputToFile;
		
		private static final int BUFFER_LEN = 16384;
		private BufferedOutputStream out;
		private int outputCounter;
		private byte[] outputBytes;
		
		public CipherProcessor(String fileName, boolean encryptMode, boolean outputToFile) {
			this.fileName = fileName;
			this.encryptMode = encryptMode;
			this.outputToFile = outputToFile;
		}
		
		public void processFile() {
			try {
				boolean useHarddrive = false;
				String harddriveDir = "G:/FourSquareCipher/";
				
				String inputPath = String.format("%s%s/%s%s.txt",
					(useHarddrive ? harddriveDir : ""),
					(encryptMode ? "input" : "output"),
					fileName,
					(encryptMode ? "" : "_enc"));
				
				String outputPath = String.format("%soutput/%s%s.txt",
						(useHarddrive ? harddriveDir : ""),
						fileName,
						(encryptMode ? "_enc" : "_dec"));
				
				OutputStream outStream;
				if (outputToFile) {
					outStream = new FileOutputStream(outputPath);
				}
				else {
					outStream = System.out;
				}
				
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputPath));
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
						
						writeIfFull();
					}
				}
				
				in.close();
				
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
					for (int i = 0; i < carriedChars.size(); ++i) {
						out.write(carriedChars.get(i));
					}
				}
				
				if (outputToFile) {
					out.close();
				}
				else {
					System.out.print("\n\n");
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
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
		

		private void writeBytes(byte[] processedBytes, List<Byte> carriedChars) throws IOException {
			outputBytes[outputCounter++] = processedBytes[0];
			
			writeIfFull();
			
			for (int j = 0; j < carriedChars.size(); ++j) {
				outputBytes[outputCounter++] = carriedChars.get(j);
				writeIfFull();
			}
			
			outputBytes[outputCounter++] = processedBytes[1];
		}
		
		private void writeIfFull() throws IOException {
			if (outputCounter == BUFFER_LEN) {
				out.write(outputBytes);
				outputBytes = new byte[BUFFER_LEN];
				outputCounter = 0;
			}
		}
	}
	
}
