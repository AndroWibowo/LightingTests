package co.haaken.shadowdemo;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.SlickException;

public class Light 
{
	private Vector2f position;
	private float range;
	private boolean shouldUpdate;
	private Color tint;
	private float alpha;
	private Color sharedColor = new Color(1f, 1f, 1f, 1f);
	private List<Polygon> shadows = new ArrayList<Polygon>();
	
	
	public class Shadow
	{
		private List<Vector2f> startVectors = new ArrayList<Vector2f>();
		private List<Vector2f> projectedVectors = new ArrayList<Vector2f>();

		public Shadow()
		{
			
		}
		
		public void addShadowFin(Vector2f start, Vector2f end)
		{
			startVectors.add(start);
			projectedVectors.add(projectToLightEdge(start));
		}
		
		public Polygon generateGeometry()
		{
			Polygon poly = new Polygon();
			
			for(Vector2f vector : startVectors)
				poly.addPoint(vector.x, vector.y);
			
			for(int i = projectedVectors.size()-1; i >= 0; i--)
				poly.addPoint(projectedVectors.get(i).x, projectedVectors.get(i).y);
			
			return poly;
		}
	}
	
	public Light (Vector2f position, float range, Color tint, float alpha) throws SlickException
	{
		this.position = position;
		this.range = range;
		this.tint = tint;
		this.alpha = alpha;
		this.shouldUpdate = true;
	}

	public Vector2f getPosition() {
		return position;
	}

	public void setPosition(Vector2f position) {
		this.position = position;
	}

	public boolean isShouldUpdate() {
		return shouldUpdate;
	}

	public void setShouldUpdate(boolean shouldUpdate) {
		this.shouldUpdate = shouldUpdate;
	}

	public void renderShadows(Graphics g, List<Block> blocks)
	{
		float length;
		int vertexCount;
		Vector2f startVert, endVert;
		
		shadows.clear();
		
		sharedColor.a = alpha;
		
		for(Block block : blocks)
		{
			// Compute the distance from the block's center (subject to change) to the light's center
			length = position.distance(new Vector2f(block.getRect().getCenter()));
			if (length <= range)
			{
    			vertexCount = block.getRect().getPointCount();
    			
    			boolean[] backFacing = new boolean[vertexCount];
    			
    			for(int pt = 0; pt < vertexCount; pt++)
    			{
    				if(pt == 0)
						startVert = new Vector2f(block.getRect().getPoint(vertexCount-1));
					else
						startVert = new Vector2f(block.getRect().getPoint(pt-1));
    				
    				endVert = new Vector2f(block.getRect().getPoint(pt));
    				
    				if(doesEdgeCastShadow(startVert, endVert))
    					backFacing[pt] = true;
    				else
    					backFacing[pt] = false;
    			}
    			
    			// Find beginning and end vertices belonging to shadow
    			int startingIndex = 0;
    			int endingIndex = 0;
    			
    			int currentEdge, nextEdge = 0;
				
				for(int i = 0; i < vertexCount; i++)
				{
					currentEdge = i;
					nextEdge = (i + 1) % vertexCount;
					
					if(backFacing[currentEdge] && !backFacing[nextEdge])
						endingIndex = currentEdge;
					if(!backFacing[currentEdge] && backFacing[nextEdge])
						startingIndex = currentEdge;
				}
				
				int shadowVertexCount;
				
				if (endingIndex > startingIndex)
					shadowVertexCount = endingIndex - startingIndex + 1;
				else
					shadowVertexCount = vertexCount + 1 - startingIndex + endingIndex;
				
				// Create triangle strip in shape of shadow
				int currentIndex = startingIndex;
				
				Shadow shadow = new Shadow();
				
				Vector2f startVector;
				Vector2f projectedVector;

				if(startingIndex < endingIndex && startingIndex - 1 != endingIndex)
					for(int i = startingIndex; i <= endingIndex; i++)
					{
						startVector = new Vector2f(block.getRect().getPoint(i));
						projectedVector = projectToLightEdge(startVector);
						
						shadow.addShadowFin(startVector, projectedVector);
					}
				else if(startingIndex - 1 == endingIndex || startingIndex > endingIndex)
					for(int i = 0; i < shadowVertexCount; i++)
					{
						startVector = new Vector2f(block.getRect().getPoint(currentIndex));
						projectedVector = projectToLightEdge(startVector);
					
						shadow.addShadowFin(startVector, projectedVector);
						
						currentIndex = (currentIndex + 1) % vertexCount;
					}
				
				shadows.add(shadow.generateGeometry());
			}
		}
		GL11.glEnable(GL11.GL_BLEND);
		g.setClip((int)(position.x - range), (int)(position.y - range), (int)range*2, (int)range*2);

		GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_DST_ALPHA);

		// Draw light gradient
		int subdivisions = 32;
		float incr = (float) (2 * Math.PI / subdivisions);
		
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			// Inner vertex
			GL11.glColor4f(tint.r, tint.g, tint.b, alpha);
			GL11.glVertex2f(position.x, position.y);
			
			// Outer vertices
			GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
			for(int i = 0; i <= subdivisions; i++)
            {
                  float angle = incr * i;

                  float x = (float) Math.cos(angle) * range + position.x;
                  float y = (float) Math.sin(angle) * range + position.y;

                  GL11.glVertex2f(x, y);
            }
		GL11.glEnd();
		
		GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ZERO);
		g.setColor(Color.transparent);
		for(Polygon shadowPoly : shadows)
			g.fill(shadowPoly);
		
		g.clearClip();
	}
	
	private boolean doesEdgeCastShadow(Vector2f start, Vector2f end)
    {   
    	float lx = (start.x + end.x) / 2;
    	float ly = (start.y + end.y) / 2;
    	Vector2f middle = new Vector2f(lx, ly);
    	
    	Vector2f tempLight = position.copy();
    	
    	Vector2f L = tempLight.sub(middle);
    	
    	Vector2f N = new Vector2f();
    	N.x = -(end.y - start.y);
    	N.y = end.x - start.x;
    	
    	if(N.dot(L) > 0)
    		return true;
    	else
    		return false;
    }
	
	private Vector2f projectToLightEdge(Vector2f startVector)
	{
		// Do some fancy math to get the far vector (the point that sits on the edge of the light's range
		float dy = startVector.y - position.y;
		float dx = startVector.x - position.x;
		
		double theta = Math.atan2(dy, dx);
		
		float farX = (float)Math.cos(theta) * (range * (float)Math.sqrt(2)) + position.x;
		float farY = (float)Math.sin(theta) * (range * (float)Math.sqrt(2)) + position.y;
		
		return new Vector2f(farX, farY);
	}
}