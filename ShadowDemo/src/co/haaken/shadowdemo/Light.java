package co.haaken.shadowdemo;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.SlickException;

public class Light 
{
	// Light Properties -------------------------------------------------------
	// Position of light
	private Vector2f position;
	
	// Outer radius of light
	private float outerRadius;

	// Inner radius of light
	private float innerRadius;
	
	// Color of the light
	private Color tint;
	
	// Should the light be recalculated/re-rendered?
	private boolean shouldUpdate;
	
	// Should the light cast shadows?
	private boolean shouldCastShadows;
	
	// Light Objects ----------------------------------------------------------
	private List<Polygon> shadows = new ArrayList<Polygon>();
	private Image renderedImage;
	
	// Other properties -------------------------------------------------------
	private static int imageScale = 1; // DEPRECATED
	
	// Padding for the image
	private int padding;
	
	// Image's origin in relation to position
	private float imageX;
	private float imageY;
	
	// Image's midpoint (includes the image scale)
	private float imageCenterX;
	private float imageCenterY;
	
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
	
	public Light (Vector2f position, Color tint, float innerRadius, float outerRadius, boolean shouldCastShadows) throws SlickException
	{
		this.position = position;
		this.tint = tint;
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
		this.shouldCastShadows = shouldCastShadows;
		this.shouldUpdate = true;
		
		this.imageX = this.position.x - (outerRadius * imageScale);
		this.imageY = this.position.y - (outerRadius * imageScale);
		
		this.imageCenterX = outerRadius * imageScale;
		this.imageCenterY = outerRadius * imageScale;
		
		this.renderedImage = new Image((int)(outerRadius*2)*imageScale, (int)(outerRadius*2)*imageScale);
	}

	public float getRange()
	{
		return outerRadius;
	}
	
	public Vector2f getPosition() 
	{
		return position;
	}

	public void setPosition(Vector2f position) 
	{
		this.position = position;
	}

	public boolean isShouldUpdate() 
	{
		return shouldUpdate;
	}

	public void setShouldUpdate(boolean shouldUpdate) 
	{
		this.shouldUpdate = shouldUpdate;
	}

	public boolean isShouldCastShadows() 
	{
		return shouldCastShadows;
	}

	public void setShouldCastShadows(boolean shouldCastShadows) 
	{
		this.shouldCastShadows = shouldCastShadows;
	}

	public void computeShadows(List<Block> blocks)
	{
		imageX = position.x - (outerRadius * imageScale);
		imageY = position.y - (outerRadius * imageScale);
		
		float length;
		int vertexCount;
		Vector2f startVert, endVert;
		
		shadows.clear();
		
		for(Block block : blocks)
		{
			// Compute the distance from the block's center (subject to change) to the light's center
			length = position.distance(new Vector2f(block.getRect().getCenter()));
			if (length <= outerRadius)
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
						// set relative pos
						startVector.x = startVector.x - imageX;
						startVector.y = startVector.y - imageY; 
						projectedVector = projectToLightEdge(startVector);
						
						shadow.addShadowFin(startVector, projectedVector);
					}
				else if(startingIndex - 1 == endingIndex || startingIndex > endingIndex)
					for(int i = 0; i < shadowVertexCount; i++)
					{
						startVector = new Vector2f(block.getRect().getPoint(currentIndex));
						// set relative pos
						startVector.x = startVector.x - imageX;
						startVector.y = startVector.y - imageY; 
						projectedVector = projectToLightEdge(startVector);
						shadow.addShadowFin(startVector, projectedVector);
						
						currentIndex = (currentIndex + 1) % vertexCount;
					}
				
				shadows.add(shadow.generateGeometry());
			}
		}
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
	
	public void renderToImage(GameContainer container)
	{
		Graphics g = container.getGraphics();
		
		g.setBackground(Color.transparent);
		g.clear();
		///*
		g.clearAlphaMap();
		g.setDrawMode(Graphics.MODE_ALPHA_MAP);
		
        // Draw light gradient
 		drawLight(Color.black, outerRadius);

		g.setColor(Color.transparent);
		for(Polygon shadowPoly : shadows)
			g.fill(shadowPoly);
		//*/
		g.setDrawMode(Graphics.MODE_ADD_ALPHA);
		
		drawLight(tint, outerRadius);
		drawLight(Color.white, outerRadius*0.2f);
		
		g.copyArea(renderedImage, 0, 0);

		g.clear();
	}
	
	public Image getRenderedImage()
	{
		return renderedImage;
	}
	
	public void drawLight(Color color, float radius)//, float radius)
	{
		int subdivisions = 32;
 		float incr = (float) (2 * Math.PI / subdivisions);
 		
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			// Inner vertex
			GL11.glColor4f(color.r, color.g, color.b, color.a);
			GL11.glVertex2f(imageCenterX, imageCenterY);
			
			// Outer vertices
			GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
			for(int i = 0; i <= subdivisions; i++)
         {
               float angle = incr * i;

               float x = (float) Math.cos(angle) * radius + imageCenterX;
               float y = (float) Math.sin(angle) * radius + imageCenterY;

               GL11.glVertex2f(x, y);
         }
		GL11.glEnd();
	}
	
	private Vector2f projectToLightEdge(Vector2f startVector)
	{
		// Do some fancy math to get the far vector (the point that sits on the edge of the light's range
		float dy = startVector.y - imageCenterX;
		float dx = startVector.x - imageCenterY;
		
		double theta = Math.atan2(dy, dx);
		
		float farX = (float)Math.cos(theta) * (outerRadius * (float)Math.sqrt(2)) + imageCenterX;
		float farY = (float)Math.sin(theta) * (outerRadius * (float)Math.sqrt(2)) + imageCenterY;
		
		return new Vector2f(farX, farY);
	}
}