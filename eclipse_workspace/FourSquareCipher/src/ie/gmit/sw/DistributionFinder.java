package ie.gmit.sw;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DistributionFinder {
	
	public static void readFile(String fileName) {
		final int READ_LEN = 4196;
		
		int[] dist = new int[256];
		
		for (int i = 0; i < dist.length; ++i) {
			dist[i] = 0;
		}
		
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(String.format("input/%s.txt", fileName)));
			byte[] inputBytes = new byte[READ_LEN];
			int bytesRead;
			int counter = 0;
			
			while ((bytesRead = in.read(inputBytes)) != -1) {
				for (byte b : inputBytes) {
					if (b > 0) ++dist[b];
					//System.out.println(((char) b) + " - " + b);
				}
			}
			
			in.close();
			
			for (int i = 0; i < dist.length; ++i) {
				System.out.printf("%c -> %d\n", (char)i, dist[i]);
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		boolean[] used = new boolean[256];
		ArrayList<Character> chars = new ArrayList<Character>();
		
		for (int i = 0; i < 256; ++i) {
			int highest = -1;
			int highestIndex = -1;
			for (int j = 0; j < 256; ++j) {
				if (dist[j] > highest && !used[j]) {
					highest = dist[j];
					highestIndex = j;
				}
			}
			
			chars.add((char)highestIndex);
			used[highestIndex] = true;
		}
		
		System.out.println("---------------");
		
		for (int i = 0; i < chars.size(); ++i) {
			System.out.println(chars.get(i) + " (char value " + (int)chars.get(i) + ")");
		}
	}
	
	public static void main(String[] args) {
		readFile("CombinedBooks");
	}
}
