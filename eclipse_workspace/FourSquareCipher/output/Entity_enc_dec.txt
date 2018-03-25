package ronan_hanley.inside_av;

/**
 * The top superclass of probably everything in the game that appears
 * on the screen; it needs an x and y location.
 * 
 * Enemies, weapons, projectiles, all extend this class.
 * @author Ronan
 */
public abstract class Entity {
	private double x;
	private double y;
	
	public Entity(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	private int roundPos(double n) {
		return (int)Math.round(n);
	}
	
	public int getX() {
		return roundPos(x);
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public int getY() {
		return roundPos(y);
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public double getXExact() {
		return x;
	}
	
	public double getYExact() {
		return y;
	}
	
	public int getCentreX() {
		return getX() + InsideAV.HALF_TILE_SIZE;
	}
	
	public int getCentreY() {
		return getY() + InsideAV.HALF_TILE_SIZE;
	}
	
	public void changeX(double change) {
		setX(getXExact() + change);
	}
	
	public void changeY(double change) {
		setY(getYExact() + change);
	}
	
	/**
	 * X location in tiles instead of pixels
	 * @return
	 */
	public int getTileX() {
		return getX() / InsideAV.TILE_SIZE;
	}
	
	/**
	 * Y location in tiles instead of pixels
	 * @return
	 */
	public int getTileY() {
		return getY() / InsideAV.TILE_SIZE;
	}
	
	/**
	 * X location in tiles instead of pixels
	 */
	public void setTileX(int tileX) {
		setX(tileX * InsideAV.TILE_SIZE);
	}
	
	/**
	 * Y location in tiles instead of pixels
	 */
	public void setTileY(int tileY) {
		setY(tileY * InsideAV.TILE_SIZE);
	}
	
}
