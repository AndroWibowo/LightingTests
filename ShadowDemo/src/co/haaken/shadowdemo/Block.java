package co.haaken.shadowdemo;

import org.newdawn.slick.geom.Rectangle;

public class Block 
{
	private Rectangle rect;
	
	public Block(float x, float y, float width, float height)
	{
		rect = new Rectangle(x, y, width, height);
	}
	
	public Rectangle getRect()
	{
		return rect;
	}
}
