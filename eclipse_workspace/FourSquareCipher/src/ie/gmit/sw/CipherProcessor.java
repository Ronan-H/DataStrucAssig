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
import java.util.concurrent.CountDownLatch;

/**
 * Handles reading from file/url and writing to a file/console,
 * feeding all bytes through the parent Cipher object.
 */
	public class CipherProcessor {
		private BufferedOutputStream out;
		private static final int numThreads = 12;
		private CipherProcessorUnit[] units;
		
		public CipherProcessor(Cipher cipher) {
			int i;
			
			units = new CipherProcessorUnit[numThreads];
			
			for (i = 0; i < numThreads; ++i) {
				units[i] = new CipherProcessorUnit(cipher);
			}
			
			for (i = 0; i < numThreads - 1; ++i) {
				units[i].setNextUnit(units[i + 1]);
			}
			
			units[numThreads - 1].setNextUnit(units[0]);
			
			for (i = 0; i < numThreads; ++i) {
				new Thread(units[i]).start();
			}
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
		public void processFile(String resourcePath, boolean encryptMode, boolean readFromURL, boolean writeToFile) throws IOException {
			int i;
			
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
			
			// init units
			CountDownLatch fileFinishedLatch = new CountDownLatch(1);
			
			for (i = 0; i < units.length; ++i) {
				units[i].initNewFile(in, out, encryptMode, fileFinishedLatch);
			}
			
			// start multithreaded encryption/decryption
			units[0].getReadLatch().countDown();
			units[0].getWriteLatch().countDown();
			// wait for file processing to be finished
			try {
				fileFinishedLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
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
	