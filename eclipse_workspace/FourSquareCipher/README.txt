
-= Ronan's Rapid Four Square Cipher Application =-

Extra features:
- Extended alphabet (64 characters including A-Z, a-z, 0-9, space and comma).
- Allows unsupported characters to be "passed through the cipher"/preserved: they are outputted without being encrypted.
-> This means if you encrypt and decrypt a file, it should be identical (unless a buffer character was added).
->> Buffer character is a space so contents appear identical.
- A sophisticated encryption/decryption approach to allow for insane speeds.
- Full key input sanitization: any string at all can be entered for a key and a logical sanitized key will always be produced.
- Option to display the cipher's four squares (containing the key and alphabet).

How encryption/decryption works:
- Files are read and written as bytes for better performance.
- Uses the idea of mapping bigrams to other bigrams instead of manipulating indices in a 2D array.
-> The idea being that we perform all the calculations for which bigrams map to which before processing the file, thus saving having to do that work for every bigram during encryption/decryption (precomputation).
-> To do this very quickly, arrays are used, since array lookups are very fast.
->> The 2 bigram characters are first both converted to "packed" form; simply 0 for A, 1 for B...up to 63 for comma, using an array lookup table.

- For encryption:
-> These two packed characters, 6 bits each, are combined into one 12 bit number using binary opations << and | (12 bits can be stored in a short).
->> (bigram part A is shifted 6 bits left, and bigram part B fills the gap on the right)
-> A lookup table is used (generated on the Cipher's creation) to encrypt the bigram.
->> This 12 bit number is used as the index, and it's corresponding value is the encrypted bigram, combined, in packed form.
-> Now the reverse is done: the leftmost 6 bits, bigram part A, are extracted using >>, and the rightmost 6 bits, bigram part B, are extracted using &
-> These 6 bit numbers are converted back to "unpacked" form (standard java chars), again using an array lookup table

- For decryption:
-> The process is identical to encryption, simply using a different lookup array (the same as the encryption array, just with indices swapped with values)
-> As such, theoretically encryption and decryption are performed in the same amount of time

- Performance
-> This method seems to perform extremely well. On my machine with SSD storage, DDR4 RAM and a 3.5GHz processor, the sample input file "WarAndPeace-LeoTolstoy.txt" can be entirely read, encrypted on the fly and written in 50-70ms, according to the timer in the application.
-> There is a memory overhead because of the lookup tables, but these are a reasonable size.


