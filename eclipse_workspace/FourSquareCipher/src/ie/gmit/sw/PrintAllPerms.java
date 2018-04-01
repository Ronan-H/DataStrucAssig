package ie.gmit.sw;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PrintAllPerms {
	
	public static void main(String[] args) throws IOException {
		String s = Cipher.ALPHABET_STRING;
		
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("./input/AllCharPerms.txt")));
		
		for (char c1 : s.toCharArray()) {
			for (char c2 : s.toCharArray()) {
				out.print(c1);
				out.print(c2);
			}
		}
		
		out.close();
	}
	
}
