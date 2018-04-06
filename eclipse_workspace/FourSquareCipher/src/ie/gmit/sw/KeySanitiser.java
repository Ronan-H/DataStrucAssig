package ie.gmit.sw;
import static ie.gmit.sw.Cipher.*;

/**
 * Converts any input from the user into a usable key for
 * the Cipher.
 */
public final class KeySanitiser {
	// the (possibly invalid) input key(s)
	private StringBuilder[] inputKeys;
	// holds the output "sanitised" key
	private String sanitizedKey = null;
	
	public KeySanitiser(StringBuilder[] inputKeys) {
		this.inputKeys = inputKeys;
		if (this.inputKeys[1] == null) this.inputKeys[1] = new StringBuilder(ALPHABET_SIZE);
	}
	
	/**
	 * First sanitises the input keys if they haven't been already,
	 * then returns the "clean" key.<br>
	 * <br>
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
	 * Sanitises the input keys in 4 distinct steps.<br>
	 * <br>
	 * The outputted key is guaranteed to be in the correct format to
	 * be used in the four square cipher, regardless of what the input
	 * key(s) are (for as much as I've tested it, anyway).<br>
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
	 * Running time: O(n)?<br>
	 * Reasoning: Depends on the running time of StringBuilder.append(), which
	 * might be O(n), since it has n characters to append. Otherwise, O(1).<br>
	 * <br>
	 * Space complexity: 0<br>
	 * Reasoning: No extra variables.
	 */
	private void equalizeKeys() {
		if (inputKeys[0].length() > ALPHABET_SIZE) {
			// append any extra characters to the second key
			inputKeys[1].append(inputKeys[0].substring(ALPHABET_SIZE));
			// remove those appended characters from the first key
			inputKeys[0].delete(ALPHABET_SIZE, inputKeys[0].length());
		}
	}
	
	/**
	 * Running time: O(n)<br>
	 * Reasoning: Running time is proportional time to the size of the
	 * input key.<br>
	 * <br>
	 * Space complexity: 0<br>
	 * Reasoning: O(1)<br>
	 * Just a few extra variables whose size remains the same when the
	 * input changes.
	 */
	private void removeUnsupportedChars() {
		int i, j;
		char c;
		
		// loop through all chars in both keys
		for (i = 0; i < 2; ++i) {
			for (j = 0; j < inputKeys[i].length(); ++j) {
				c = inputKeys[i].charAt(j);
				boolean unsupported = c < 0 || c > 127 || PACKED_CHARS[(int) c] == UNKNOWN_PLACEHOLDER_PACKED;
				
				if (unsupported) {
					// unsupported character; delete it
					inputKeys[i].deleteCharAt(j);
					// move j back one since all chars were shuffled back 1
					--j;
				}
			}
		}
	}
	
	/**
	 * Running time: O(n^2)<br>
	 * Reasoning: Compares every char in each key to every other
	 * char, giving n^2.<br>
	 * <br>
	 * Space complexity: 0<br>
	 * Reasoning: O(1)<br>
	 * Just a few extra variables whose size remains the same when the
	 * input changes.
	 */
	private void removeDuplicateChar() {
		int i, j, k;
		StringBuilder inputKey;
		
		for (i = 0; i < 2; ++i) {
			inputKey = inputKeys[i];
			for (j = 0; j < inputKey.length(); ++j) {
				for (k = 0; k < inputKey.length(); ++k) {
					// char isn't be a duplicate of itself
					if (k == j) continue;
					
					if (inputKey.charAt(k) == inputKey.charAt(j)) {
						// duplicate character found
						inputKey.deleteCharAt(k);
						// move k back one since all chars were shuffled back 1
						--k;
					}
				}
			}
		}
	}
	
	/**
	 * Running time: O(n^2)<br>
	 * Reasoning: Loops through the alphabet and searches the key
	 * each time for that character, giving n^2.
	 * <br>
	 * Space complexity: 0<br>
	 * Reasoning: O(1)<br>
	 * Just a few extra variables whose size remains the same when the
	 * input changes.
	 */
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
					// char already included in the key; no need to add it
					if (inputKey.charAt(k) == c) continue outerLoop;
				}
				
				// character not found in the key; append it
				inputKey.append(c);
			}
		}
	}
	
}
