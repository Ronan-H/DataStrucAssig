package ie.gmit.sw;

public class Runner {
	
	public void go() {
		new Menu().go();
	}
	
	public static void main(String[] args) {
		Runner runner = new Runner();
		runner.go();
	}
	
}
