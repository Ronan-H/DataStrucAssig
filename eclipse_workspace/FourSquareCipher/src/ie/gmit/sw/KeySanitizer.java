package ie.gmit.sw;
import static ie.gmit.sw.Cipher.*;

public final class KeySanitizer {
	private StringBuilder[] inputKeys;
	private String sanitizedKey = null;
	
	public KeySanitizer(StringBuilder[] inputKeys) {
		this.inputKeys = inputKeys;
		if (this.inputKeys[1] == null) this.inputKeys[1] = new StringBuilder(ALPHABET_SIZE);
	}
	
	/**
	 * First sanitizes the input keys if they haven't been already,
	 * then returns the "clean" key.
	 * 
	 * This works similar to the singleton design pattern.
	 * @return A "clean" key, ready to be used in a four square cipher.
	 */
	public String getSanitizedKey() {
		if (sanitizedKey == null) {
			sanitizeKeys();
		}
		
		return sanitizedKey;
	}
	
	/**
	 * Sanitizes the input keys in 3 distinct steps.
	 * 
	 * The outputted key is guaranteed to be in the correct format to
	 * be used in the four square cipher, regardless of what the input
	 * key(s) are.
	 */
	private void sanitizeKeys() {
		// Step 1: Spill any additional chars from key 1 to key 2
		equalizeKeys();
		
		// Step 2: Remove any characters not in the cipher's alphabet
		removeUnsupportedChars();
		
		// Step 3: Remove duplicate characters
		removeDuplicateChar();
		
		// Step 4: Append characters, if necessary, to get the right key length
		addPadding();
		
		sanitizedKey = inputKeys[0].append(inputKeys[1]).toString();
	}
	
	/**
	 * Running time: O(n)?
	 * Reasoning: Depends on the running time of StringBuilder.append(), which
	 * might be O(n).
	 * 
	 * Space complexity: 0
	 * Reasoning: No extra variables.
	 */
	private void equalizeKeys() {
		if (inputKeys[0].length() > ALPHABET_SIZE) {
			inputKeys[1].append(inputKeys[0].substring(ALPHABET_SIZE));
			inputKeys[1].delete(ALPHABET_SIZE, inputKeys[1].length());
		}
	}
	
	/**
	 * Running time: O(n)
	 * Reasoning: Running time is proportional time to the size of the
	 * input key.
	 * 
	 * Space complexity: 0
	 * Reasoning: O(1)
	 * Just a few extra variables whose size remains the same when the
	 * input changes.
	 */
	private void removeUnsupportedChars() {
		int i, j;
		int packedChar;
		char c;
		
		for (i = 0; i < 2; ++i) {
			//long startNS = System.nanoTime();
			// inputKeys[i] = new StringBuilder(inputKeys[i].toString().replaceAll("[^A-Za-z0-9 ,]", ""));
			
			for (j = 0; j < inputKeys[i].length(); ++j) {
				c = inputKeys[i].charAt(j);
				packedChar = (c < 0 ? -1 : PACKED_CHARS[(int) c]);
				
				if (packedChar == -1) {
					// unsupported character
					inputKeys[i].deleteCharAt(j);
					--j;
				}
			}
			
			//System.out.printf("Removed invalid chars in %dns.%n", System.nanoTime() - startNS);
		}
	}
	
	private void removeDuplicateChar() {
		int i, j, k;
		StringBuilder inputKey;
		
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
	}
	
	private void addPadding() {
		int i, j, k;
		StringBuilder inputKey;
		char c;
		
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
	}
	
}
