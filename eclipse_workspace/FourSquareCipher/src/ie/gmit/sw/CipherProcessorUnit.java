package ie.gmit.sw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class CipherProcessorUnit implements Runnable{
	// how many bytes to be read into the buffer each time
	// the value of 8192 seems most efficient
	private static final int BUFFER_LEN = 8192;

	private BufferedInputStream in;
	private BufferedOutputStream out;
	private byte[] bufferBytes;
	private Cipher cipher;
	private boolean encryptMode;
	private boolean running;
	
	private CipherProcessorUnit nextUnit;
	private CountDownLatch writeLatch;
	private CountDownLatch readLatch;
	private CountDownLatch fileFinishedLatch = null;
	
	public CipherProcessorUnit(Cipher cipher) {
		this.cipher = cipher;
		bufferBytes = new byte[BUFFER_LEN];
	}
	
	public void run() {
		int bytesRead;
		int i;
		
		running = true;
		while (running) {
			try {
				if (fileFinishedLatch == null || fileFinishedLatch.getCount() == 0) {
					readLatch = new CountDownLatch(1);
					writeLatch = new CountDownLatch(1);
				}
				
				readLatch.await();
				bytesRead = in.read(bufferBytes);
				
				if (bytesRead == -1) {
					// no more bytes to be read
					// trigger file finished
					writeLatch.await();
					fileFinishedLatch.countDown();
					// wait for next file
					continue;
				}
				else {
					readLatch = new CountDownLatch(1);
					nextUnit.getReadLatch().countDown();
				}
				
				if (bytesRead % 2 != 0) {
					// off number of bytes; add the buffer character (space)
					bufferBytes[bytesRead++] = ' ';
				}
				
				// convert all bytes in the buffer to "packed form"
				for (i = 0; i < bytesRead; ++i) {
					if (bufferBytes[i] < 0) {
						// non ASCII character
						bufferBytes[i] = Cipher.Q_MARK_INDEX;
					}
					else {
						bufferBytes[i] = Cipher.PACKED_CHARS[bufferBytes[i]];
					}
				}
				
				// encrypt/decrypt byte pairs in place
				for (i = 0; i < bytesRead; i += 2) {
					if (encryptMode) {
						cipher.encryptBigram(bufferBytes, i);
					}
					else {
						cipher.decryptBigram(bufferBytes, i);
					}
				}
				
				// convert all bytes back to "unpacked" form (ASCII chars)
				for (i = 0; i < bytesRead; ++i) {
					bufferBytes[i] = Cipher.UNPACKED_CHARS[bufferBytes[i]];
				}
				
				writeLatch.await();
				out.write(bufferBytes, 0, bytesRead);
				writeLatch = new CountDownLatch(1);
				nextUnit.getWriteLatch().countDown();
			} catch (IOException e) {
				e.printStackTrace();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initNewFile(BufferedInputStream in,
							BufferedOutputStream out,
							boolean encryptMode,
							CountDownLatch fileFinishedLatch) {
		this.in = in;
		this.out = out;
		this.encryptMode = encryptMode;
		this.fileFinishedLatch = fileFinishedLatch;
	}
	
	public void setNextUnit(CipherProcessorUnit nextUnit) {
		this.nextUnit = nextUnit;
	}
	
	public CountDownLatch getReadLatch() {
		return readLatch;
	}
	
	public void setReadLatch(CountDownLatch latch) {
		this.readLatch = latch;
	}
	
	public CountDownLatch getWriteLatch() {
		return writeLatch;
	}
	
	public void setWriteLatch(CountDownLatch latch) {
		this.writeLatch = latch;
	}
	
}