package ie.gmit.sw;

public class Cipher {
	private short[][][] fourSq;
	public byte[] packedChars;
	public short[] unpackedChars;
	private short[] encryptArr;
	private short[] decryptArr;
	private char[][] sqChars;
	
	public Cipher(String key) {
		packedChars = new byte[256];
		unpackedChars = new short[256];
		
		for (short s = 0; s < 128; ++s) {
			packedChars[s] = packChar(s);
		}
		
		for (short s = 0; s < 128; ++s) {
			unpackedChars[s] = unpackChar(s);
			//System.out.printf("%3d: %c\n", s, (char)unpackedChars[s]);
		}
		
		encryptArr = new short[4096];
		decryptArr = new short[4096];
		
		// populate top left / bottom right quadrants
		fourSq = new short[3][8][8];
		
		sqChars = new char[16][16];
		
		short charCounter = 0;
		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				short compactVal = charCounter++;
				fourSq[0][i][j] = (short)charCounter;
				char charVal = (char)unpackedChars[compactVal];
				
				sqChars[i][j] = charVal;
				sqChars[i + 8][j + 8] = charVal;
			}
		}
		
		// populate top right / bottom left quadrants with the encryption key
		int keyIndex = 0;
		for (int i = 1; i <= 2; ++i) {
			for (int j = 0; j < 8; ++j) {
				for (int k = 0; k < 8; ++k) {
					char keyChar = key.charAt(keyIndex++);
					fourSq[i][j][k] = packedChars[(short) keyChar];
					
					if (i == 1) {
						sqChars[j][k + 8] = keyChar;
					}
					else {
						sqChars[j + 8][k] = keyChar;
					}
				}
			}
		}
		
		int arrCounter = 0;
		// generate the short arrays used for encryption/decryption
		for (int c1y = 0; c1y < 8; ++c1y) {
			for (int c1x = 0; c1x < 8; ++c1x) {
				for (int c2y = 0; c2y < 8; ++c2y) {
					for (int c2x = 0; c2x < 8; ++c2x) {
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
		
		// Collision
		// 1246
		// 1388
		
		System.out.println("En test: " + encryptArr[(short) (packedChars['e'] << 6 | packedChars['p'])]);
		System.out.println("En test: " + encryptArr[(short) (packedChars['u'] << 6 | packedChars['u'])]);
		
		// System.out.println("Test: " + encryptArr[2000]);
	}
	
	public byte[] encryptPackedChars(byte a, byte b) {
		byte[] encrypted = new byte[2];
		short combined = (short) (a << 6 | b);
		short combinedResult = encryptArr[combined];
		
		encrypted[0] = (byte) unpackedChars[combinedResult >> 6];
		encrypted[1] = (byte) unpackedChars[combinedResult & 0x3F];
		
		return encrypted;
	}
	
	public byte[] encryptChars(byte a, byte b) {
		byte[] encrypted = new byte[2];
		short combined = (short) (packedChars[a] << 6 | packedChars[b]);
		System.out.println("Comb: " + combined);
		short combinedResult = encryptArr[combined];
		
		encrypted[0] = (byte) unpackedChars[(combinedResult >> 6)];
		encrypted[1] = (byte) unpackedChars[(combinedResult & 0x3F)];
		
		return encrypted;
	}
	
	public byte[] decryptChars(byte a, byte b) {
		byte[] decrypted = new byte[2];
		short combined = (short) (a >> 6 | b);
		short combinedResult = decrypted[combined];
		
		decrypted[0] = (byte) (combinedResult << 6);
		decrypted[1] = (byte) (combinedResult & 0x3F);
		
		return decrypted;
	}
	
	public byte[] decryptPackedChars(byte a, byte b) {
		byte[] decrypted = new byte[2];
		short combined = (short) (a << 6 | b);
		short combinedResult = decryptArr[combined];
		
		decrypted[0] = (byte) unpackedChars[combinedResult >> 6];
		decrypted[1] = (byte) unpackedChars[combinedResult & 0x3F];
		
		return decrypted;
	}
	
	private byte packChar(short c) {
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
	
	private short unpackChar(short c) {
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
	
}
