
# Ronan's Rapid Four Square Cipher Application

### How encryption/decryption works:
* Input is read in as a buffer of bytes
* Uses the idea of mapping bigrams to other bigrams (ie. "TH" -> "ES") directly instead of manipulating indexes in a 2D array; precomputation.
* Lookup tables are generated when the cipher is initialised.
  * Ordinary Java arrays are used since they are very fast.
  * The 2 bigram characters are first both converted to "packed" form; simply 0 for A, 1 for B...up to 80 for \n, using another lookup table.

### For encryption:
* These two packed characters, 7 bits each, are combined into one 14 bit number using binary operations << and | (14 bits can be stored in a single short).
  * (bigram part A is shifted 7 bits left, and bigram part B fills the gap on the right)
  * Eg. Bits xxxxxxx and yyyyyyy join to become xxxxxxxyyyyyyy
* A lookup table is used to encrypt the bigram.
  * This 14 bit number is used as the index, and it's corresponding value is the encrypted bigram, combined, in packed form.
* The reverse is now done on this encrypted value: the leftmost 7 bits, bigram part A, are extracted using >>, and the rightmost 7 bits, bigram part B, are extracted using &.
* These 7 bit numbers are converted back to "unpacked" form (standard Java chars, UTF), again using an array lookup table.

### For decryption:
* The process is the same as for encryption, simply using a different lookup array (the same as the encryption array, just with indexes swapped with values).

### Extra features:
* Extended alphabet (81 characters including A-Z, a-z, 0-9, some symbols, and \n [newlines are treated as characters and encrypted]).
* Option to display the cipher's key and print the four squares.
* A sophisticated encryption/decryption approach to allow for insane speeds.
* Unsupported characters are replaced with '?' instead of being filtered out.
* Full key input sanitisation: any string at all can be entered for a key and a logical sanitised key will always be produced.

### Performance
* This method seems to perform extremely well.
* On my machine with a 3.5GHz processor, SSD storage, and DDR4 RAM, the sample input file "WarAndPeace-LeoTolstoy.txt" can be entirely read, encrypted on the fly and written in 15 milliseconds, according to the timer included in the application.
* Encrypting War and Peace times 10 completed in ~95 milliseconds, so it scales very well too.
* Decryption should complete in a similar time, perhaps slightly faster, as there is guaranteed to be no unsupported characters.
* There is a memory overhead because of the lookup tables, but these are a reasonable size. A large buffer is also used to maximize speeds.
  * The four lookup tables together take up: (128 + 81 + (10321 * 2 * 2)) / 1024 = 40.52 kilobytes.

**Note:** The large buffer size seems to cause only the end of large files to be printed to the console on Windows, but this doesn't seem to happen on Linux. The problem goes away when the buffer size is reduced down to 8192 bytes.
