package ie.gmit.sw;

import static java.lang.System.out;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Handles reading from file/url and writing to a file/console,
 * feeding all bytes through the parent Cipher object.
 */
public class CipherProcessor {
	private static final int BUFFER_LEN = 8192;
	// the input byte buffer
	private byte[] buffer;
	private Cipher cipher;
	
	public CipherProcessor(Cipher cipher) {
		this.cipher = cipher;
		buffer = new byte[BUFFER_LEN];
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
	 * 
	 * @param resourcePath Patht to the resource file/URL
	 * @param encryptMode true for encrypt, false for decrypt
	 * @param readFromURL true to read from URL, false to read from file
	 * @param writeToFile true to write to file, false to write to console
	 * @throws IOException
	 */
	public void processFile(String resourcePath, boolean encryptMode, boolean readFromURL, boolean writeToFile) throws IOException {
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
		BufferedOutputStream out = new BufferedOutputStream(outStream);
		
		// fill the buffer until no more bytes are available
		while ((bytesRead = in.read(buffer)) != -1) {
			if (bytesRead % 2 != 0) {
				// odd number of bytes; add the buffer character (space)
				buffer[bytesRead++] = ' ';
			}
			
			// encrypt/decrypt byte pairs in place
			if (encryptMode) {
				cipher.encryptAll(buffer, bytesRead);
			}
			else {
				cipher.decryptAll(buffer, bytesRead);
			}
			
			out.write(buffer, 0, bytesRead);
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
