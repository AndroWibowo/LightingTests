package co.haaken.shadowdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

public class ShadowDemo extends BasicGame
{
	// Base settings
	static int width = 1280;
    static int height = 800;
    static boolean fullscreen = false;
    static boolean showFPS = true;
    static boolean useVSync = true;
    static String title = "Shadows Demo";
	
    private Random random = new Random();
    
    private List<Block> blocks = new ArrayList<Block>();
    private List<Light> lights = new ArrayList<Light>();
    
    private Light dynamicLight;
    
	public ShadowDemo(String title)
	{
		super(title);
	}
	
	@Override
    public void init(GameContainer gc) throws SlickException 
    {	
		resetBlocks(gc);
		resetLights(gc);
    }
 
    @Override
    public void update(GameContainer gc, int delta) throws SlickException 
    {
    	// Reset lights when space bar is pressed
    	if (gc.getInput().isKeyPressed(Input.KEY_SPACE)) 
            resetLights(gc);
    	
    	// Reset blocks when R is pressed
    	if (gc.getInput().isKeyPressed(Input.KEY_R)) 
            resetBlocks(gc);
  
    	// Mouse position
    	float[] mousePos = {gc.getInput().getMouseX(), gc.getInput().getMouseY()};
    	
    	if(gc.getInput().isMousePressed(Input.MOUSE_LEFT_BUTTON))
    		addLight(mousePos[0], mousePos[1]);
    	
    	dynamicLight = lights.get(lights.size()-1);
    	
    	if(dynamicLight.getPosition().x != mousePos[0] || dynamicLight.getPosition().y != mousePos[1] || dynamicLight.isShouldUpdate())
		{
			dynamicLight.getPosition().x = gc.getInput().getMouseX();
			dynamicLight.getPosition().y = gc.getInput().getMouseY();
			dynamicLight.computeShadows(blocks);
			dynamicLight.renderToImage(gc);
		}
    	
    	for(Light light : lights)
    		if(light.isShouldUpdate())
    		{
    			light.computeShadows(blocks);
    			light.renderToImage(gc);
    		}			
    }
 
    @Override
    public void render(GameContainer gc, Graphics g) throws SlickException 
    {    		
    	g.setDrawMode(Graphics.MODE_ADD_ALPHA);
    	// Iterate through each light
    	for(Light light : lights)
    	{
    		g.drawImage(light.getRenderedImage(), light.getPosition().x - (light.getRenderedImage().getWidth()/2), light.getPosition().y - (light.getRenderedImage().getHeight()/2));
    	}

    	g.setDrawMode(Graphics.MODE_NORMAL);
    	
    	// Render the blocks
    	for(Block block : blocks)
		{
    		g.setColor(Color.white);
    		g.fill(block.getRect());
    	}

    	g.setDrawMode(Graphics.MODE_NORMAL);
    	g.setColor(Color.white);
        g.drawString("Lights: " + lights.size(), 10, 25);
    }

    // Add a new light
    public void addLight(float x, float y) throws SlickException
    {
    	if(lights.size() > 0)
    		lights.get(lights.size() - 1).setShouldUpdate(false);
    	Color newColor = new Color((random.nextFloat() + 0.5f) - 0.5f, (random.nextFloat() + 0.5f) - 0.5f, (random.nextFloat() + 0.5f) - 0.5f, 1.0f);
        //lights.add(new Light(new Vector2f(x, y), 600f, newColor, newColor.a));
    	lights.add(new Light(new Vector2f(x, y), newColor, 64, 384, true));
    }
    
    public void resetLights(GameContainer container) throws SlickException 
    {
        //clear the lights and add a new one with default scale
        lights.clear();
        addLight(container.getInput().getMouseX(), container.getInput().getMouseY());
    }

    
    public void resetBlocks(GameContainer container)
    {
    	if(blocks.isEmpty())
    	{
    		// Generate new blocks
        	for(int i = 0; i < 5; i++)
    			blocks.add(new Block(random.nextInt(container.getWidth()), random.nextInt(container.getHeight()), 100f, 50f));	
    	}
    	else
    	{
    		for(Block block : blocks)
    			block.getRect().setLocation(random.nextInt(container.getWidth()), random.nextInt(container.getHeight()));  
    		for(Light light : lights)
    			light.setShouldUpdate(true);
    	}		
    }
    
    public static void main(String[] args) throws SlickException 
    {
        AppGameContainer app = new AppGameContainer(new ShadowDemo(title));
        app.setDisplayMode(width, height, fullscreen);
        app.setVSync(useVSync);
        app.setSmoothDeltas(useVSync);
        app.setShowFPS(showFPS);
        app.start();
    }

}
