/***************************************************************************
	                     FishWorld.java
	                   ------------------
	   description          : point and click game with fishes
	   copyright            : (C) 2001,2002 by Stephan Uhlmann
	   email                : su@su2.info
	***************************************************************************/
	
/***************************************************************************
	*                                                                         *
	*   This program is free software; you can redistribute it and/or modify  *
	*   it under the terms of the GNU General Public License as published by  *
	*   the Free Software Foundation; either version 2 of the License, or     *
	*   (at your option) any later version.                                   *
	*                                                                         *
	***************************************************************************/

import java.applet.Applet;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

/**
	 * FishWorld, a point and click game with fishes <br>
	 * Copyright (c) 2001,2002 by Stephan Uhlmann <a href="mailto:su@su2.info">&lt;su@su2.info&gt;</a> <br>
	 * published under the GPL
	 */

public class FishWorld extends Applet implements Runnable
{
	Thread myThread;				// thread of ourself
	boolean threadRunning;				// true while thread is running
	boolean threadQuit;				// don't quit thread run() until this is true
	Image dbImage;					// double buffer image
	Graphics dbGraphics;				// double buffer graphics
	Color myFgColor, myBgColor;			// foreground and background color
	Font myFont;					// my font
	boolean mouseClicked;				// true when mouse was clicked
	int mouseClickX, mouseClickY;			// click position
	int mouseClickButton;				// button clicked (1=left, 2=right)
	int clickLock;					// don't allow click after game over for some time
	boolean keyPressed;				// true when a key was pressed
	char keyPressedChar;				// the last key pressed as a char
	Vector fishes;					// all fishes currently alive (or just dying), Vector<Fish>
	Vector fishImages;				// the fish images even=left, odd=right, Vector<Image>
	Vector bubImages;				// the bubble animation, Vector<Image>
	Image bg01,bg02,bg03,bg04,bg11,bg12,bg13,bg14;	// 1=before, 0=behind the fishes
	Image dynamiteImage;				// the dynamite
	int dynamiteCount;				// counter of dynamite
	int score;					// the current score
	Highscore highscore;				// highscore table
	String name;					// name of the player
	int gameState;					// see GAME_STATE_*
	int loadProgress;				// load progress (0..27)


	public static final int GAME_STATE_LOADING			= 0;
	public static final int GAME_STATE_BEFORE_GAME			= 1;
	public static final int GAME_STATE_IN_GAME			= 2;
	public static final int GAME_STATE_AFTER_GAME_HIGHSCORE	= 3;
	public static final int GAME_STATE_AFTER_GAME			= 4;


	/**
	  * returns info about this applet
	  */
	public String getAppletInfo()
	{
		return "FishWorld 1.2, (c) 2001,2002 by Stephan Uhlmann <su@su2.info>, licensed under the GPL";
	}

	/**
	  * initializes the applet
	  */
	public void init()					// implemented from Applet
	{
		gameState=GAME_STATE_LOADING;
		myThread=null;
		threadRunning=false;
		dbImage = createImage(getSize().width,getSize().height);	// double buffer
		dbGraphics = dbImage.getGraphics();
		myFont = new Font("Courier",Font.BOLD,24);
		dbGraphics.setFont(myFont);
		myFgColor = new Color(64,64,255);
		myBgColor = new Color(0,0,128);

		mouseClicked=false;
		clickLock=0;
		addMouseListener(new FishWorldMouseAdapter());
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	 	keyPressed=false;
		addKeyListener(new FishWorldKeyAdapter());

		highscore=new Highscore();
		name="";

		// graphics are loaded later in run() to display progress bar
	}

	/**
	  * starts applet by forking a new Thread of ourself and starting this Thread
	  */
	public void start()					// implemented from Applet
	{
		if (myThread == null)				// if there is none
		{
			myThread = new Thread(this);		// create new thread of ourself
		}
		threadQuit=false;
		threadRunning=true;
		myThread.start();
		myThread.setPriority(Thread.MAX_PRIORITY);

		showStatus(getAppletInfo());			// show info in browser status bar
	}

	/**
	  * stops the Applet Thread by setting threadQuit to true and waiting
	  * until the Thread exists its run loop
	  */
	public void stop()					// implemented from Applet
	{
		if (myThread != null && myThread.isAlive())
		{
			threadQuit=true;			// signal thread to quit
			while (threadRunning)			// and wait until it has quit
			{
				try { Thread.sleep(20); }
					catch (InterruptedException e) { }
			};
			myThread=null;
			gameState=GAME_STATE_BEFORE_GAME;
		}
	}


	/**
	  * runs the Applet Thread, loops infinite until theadQuit is set to true
	  */
	public void run()					// implemented from Thread (Runnable)
	{
		int i;

		if (gameState==GAME_STATE_LOADING)		// loading...
		{
			loadGraphics();				// load images
			fishes = new Vector();
			for (i=0; i<6; i++) newFish();		// create some fishes
			gameState=GAME_STATE_BEFORE_GAME;	// ready for game
		}

		while (threadQuit==false)			// set to true to quit thread
		{

			if ((gameState==GAME_STATE_BEFORE_GAME)
			|| (gameState==GAME_STATE_AFTER_GAME))	// before or after game
			{
				if (clickLock>0) clickLock--;
				if ((mouseClicked) && (clickLock==0))	// when mouse clicked, start game
				{
					fishes.removeAllElements();	// create new fishes
					for (i=0; i<6; i++) newFish();
					score=0;
					dynamiteCount=3;
					gameState=GAME_STATE_IN_GAME;	// now in game
					mouseClicked=false;
				}
				moveFishes();			// move the fishes around
				repaint();			// and paint everything again
			}

			if (gameState==GAME_STATE_IN_GAME)	// in game
			{
				if (mouseClicked)
				{
					checkFishKill();	// check if we hit a fish
					mouseClicked=false;
				}
				moveFishes();			// move the fishes around
				repaint();			// and paint everything again
			}

			if (gameState==GAME_STATE_AFTER_GAME_HIGHSCORE)	// highscore dialog
			{
				keyPressed=false;
				keyPressedChar=' ';
				while (keyPressedChar!='÷')
				{
					while (keyPressed==false)
					{
						moveFishes();	// move the fishes around
						repaint();
						try { Thread.sleep(50); }	// sleep for 50 ms -> approx. 20 fps 
							catch (InterruptedException e) { }
					}
					if ((keyPressedChar!='÷') && (keyPressedChar!='«') && (name.length()<8))
						name=name+keyPressedChar;
					if ((keyPressedChar=='«') && (name.length()>0)) name=name.substring(0,name.length()-1);
					keyPressed=false;
				}
				highscore.addHighscore(score, name);
				gameState=GAME_STATE_AFTER_GAME;
			}

			try { Thread.sleep(50); }		// sleep for 50 ms -> approx. 20 fps 
				catch (InterruptedException e) { }

		}	// while

		threadRunning=false;
	}

	/**
	  * paints the Applet, just calls update()
	  */
	public void paint(Graphics g)
	{
		update(g);
	}

	/**
	  * paints the Applet depending from current gameState,
	  * uses double buffering
	  */
	public void update(Graphics g)
	{
		String s,s2;
		int w,h,w2,h2,i,y;

		w=getSize().width; w2=w/2;
		h=getSize().height; h2=h/2;

		dbGraphics.setColor(myBgColor);
		dbGraphics.fillRect(0,0,w,h);			// clearscreen
		dbGraphics.setColor(myFgColor);

		if (gameState==GAME_STATE_LOADING)		// while loading...
		{
			s = "Loading graphics...";
			dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,h2-dbGraphics.getFontMetrics().getHeight()/2);
			// 27 is the max. loadProgress - 1
			dbGraphics.fillRect(w2/2,h2+10,loadProgress*(w2/2/27),20);	// progress bar left
			dbGraphics.fillRect(w2/2*3-loadProgress*(w2/2/27),h2+10,loadProgress*(w2/2/27),20);	// progress bar right
		}

		if (gameState>GAME_STATE_LOADING)		// only draw when finished loading
								// (avoids null pointer exceptions for images)
		{
			// background behind fishes
			dbGraphics.drawImage(bg01,w2-190,h-118,this);
			dbGraphics.drawImage(bg02,w2-234,h-149,this);
			dbGraphics.drawImage(bg03,w2+151,h-87,this);
			dbGraphics.drawImage(bg04,w2+210,h-32,this);

			// the fishes
			drawFishes(dbGraphics);

			// background before fishes
			dbGraphics.drawImage(bg11,w2-260,h-82,this);
			dbGraphics.drawImage(bg12,w2-208,h-119,this);
			dbGraphics.drawImage(bg13,w2-160,h-32,this);
			dbGraphics.drawImage(bg14,w2-76,h-103,this);

			if (gameState==GAME_STATE_BEFORE_GAME)	// before game
			{
				s = "Click to start game";
				dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,h2-dbGraphics.getFontMetrics().getHeight()/2);
			}

			if (gameState==GAME_STATE_IN_GAME)		// in game
			{
				s = "" + score;
				dbGraphics.drawString(s,w-dbGraphics.getFontMetrics().stringWidth(s),dbGraphics.getFontMetrics().getHeight()); // score left
				for (i=0;i<dynamiteCount;i++)
				{
					dbGraphics.drawImage(dynamiteImage,30*i,5,this); // dynamite left
				}
			}
 
			if (gameState==GAME_STATE_AFTER_GAME_HIGHSCORE)	// highscore dialog
			{
				s = "" + score;
				dbGraphics.drawString(s,w-dbGraphics.getFontMetrics().stringWidth(s),dbGraphics.getFontMetrics().getHeight());

				y=dbGraphics.getFontMetrics().getHeight()*2;
				s = "New Highscore!";
				dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,y);
				y=y+dbGraphics.getFontMetrics().getHeight()*2;
				s = "Enter your name";
				dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,y);
				y=y+dbGraphics.getFontMetrics().getHeight();
				s = name + "_";
				dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,y);
			}

			if (gameState==GAME_STATE_AFTER_GAME)	// after game
			{
				y=dbGraphics.getFontMetrics().getHeight()*2;

				s = "Your score: " + score;
				dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,y);
				y=y+dbGraphics.getFontMetrics().getHeight();

				s = "Click for a new Game";
				dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,y);
				y=y+dbGraphics.getFontMetrics().getHeight();

				y=y+dbGraphics.getFontMetrics().getHeight();

				for (i=0;i<=9;i++)
				{
					s2 = "" + (i+1);
					while (s2.length()<2) s2=" "+s2;
					s=s2+". ";

					s2 = highscore.name(i);
					while (s2.length()<8) s2=s2+" ";
					s=s+s2+"  ";

					s2 = ""+ highscore.points(i);
					while (s2.length()<5) s2=" "+s2;
					s=s+s2;

					dbGraphics.drawString(s,w2-dbGraphics.getFontMetrics().stringWidth(s)/2,y);
					y=y+dbGraphics.getFontMetrics().getHeight();
				}

			}
		} // if (gameState>GAME_STATE_LOADING)

		g.drawImage(dbImage,0,0,this);		// draw double buffer to screen

	}
	
	/**
	  * loads the graphics, 
	  * increments loadProgress and calls repaint() after every loaded image
	  * (to display progress bar)
	  */
	public void loadGraphics()
	{
		URL anURL;
		Image anImage;
		int i;

		fishImages = new Vector();			// the fishes
		bubImages = new Vector();			// the bubbles

		loadProgress=0;
		repaint();					// draw progress bar

		try
		{
			// load background images
			anURL = new URL(getCodeBase(),"graphics/back01.gif");
			showStatus("loading file: "+anURL.toString());
			bg01 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg01, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back02.gif");
			showStatus("loading file: "+anURL.toString());
			bg02 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg02, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back03.gif");
			showStatus("loading file: "+anURL.toString());
			bg03 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg03, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back04.gif");
			showStatus("loading file: "+anURL.toString());
			bg04 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg04, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back11.gif");
			showStatus("loading file: "+anURL.toString());
			bg11 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg11, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back12.gif");
			showStatus("loading file: "+anURL.toString());
			bg12 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg12, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back13.gif");
			showStatus("loading file: "+anURL.toString());
			bg13 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg13, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			anURL = new URL(getCodeBase(),"graphics/back14.gif");
			showStatus("loading file: "+anURL.toString());
			bg14 = getToolkit().getImage(anURL);
			try { while (!prepareImage(bg14, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			// the dynamite image
			anURL = new URL(getCodeBase(),"graphics/dynamite.gif");
			showStatus("loading file: "+anURL.toString());
			dynamiteImage = getToolkit().getImage(anURL);
			try { while (!prepareImage(dynamiteImage, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
			loadProgress++; repaint();

			// the bubbles
			for (i=0; i<=6; i++)
			{
				anURL = new URL(getCodeBase(),"graphics/exp"+String.valueOf(i)+".gif");
				showStatus("loading file: "+anURL.toString());
				anImage = getToolkit().getImage(anURL);
				try { while (!prepareImage(anImage, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
				bubImages.addElement(anImage);
				loadProgress++; repaint();
			}
	  
			// the fish images, from left and from right
			for (i=1; i<=6; i++)
			{
				anURL = new URL(getCodeBase(),"graphics/fish"+String.valueOf(i)+"l.gif");
				showStatus("loading file: "+anURL.toString());
				anImage = getToolkit().getImage(anURL);
				try { while (!prepareImage(anImage, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
				fishImages.addElement(anImage);
				loadProgress++; repaint();
				anURL = new URL(getCodeBase(),"graphics/fish"+String.valueOf(i)+"r.gif");
				showStatus("loading file: "+anURL.toString());
				anImage = getToolkit().getImage(anURL);
				try { while (!prepareImage(anImage, this)) {Thread.sleep(20);} } catch (InterruptedException e) {}
				fishImages.addElement(anImage);
				loadProgress++; repaint();
			}

		} catch (MalformedURLException e)
		{
			String errorMsg = "Parse error: "+e;
			showStatus(errorMsg);
			System.err.println(errorMsg);
		}

	}

	/**
	  * creates a new Fish and adds him to the fishes Vector
	  */
	public void newFish()
	{
		Fish aFish;

		aFish = new Fish();
		aFish.type=(int)(Math.random()*6)+1;		// type: 1..6
		aFish.direction=(int)(Math.random()*2)*2-1;	// direction: -1 or 1
		if (aFish.direction==1) aFish.swimImage=(Image)fishImages.elementAt((aFish.type-1)*2);
			else aFish.swimImage=(Image)fishImages.elementAt((aFish.type-1)*2+1);
		aFish.width=aFish.swimImage.getWidth(null); aFish.height=aFish.swimImage.getHeight(null);
		if (aFish.direction==1) aFish.x=-aFish.width;
			else aFish.x = getSize().width;
		aFish.y=(int)(Math.random()*(getSize().height-aFish.height));
		switch(aFish.type)				// the different fishes are differently fast
		{
			case 1: aFish.speed=(int)(Math.random()*2)+4;break;
			case 2: aFish.speed=(int)(Math.random()*2)+3;break;
			case 3: aFish.speed=(int)(Math.random()*2)+1;break;
			case 4: aFish.speed=(int)(Math.random()*2)+3;break;
			case 5: aFish.speed=(int)(Math.random()*2)+2;break;
			case 6: aFish.speed=(int)(Math.random()*2)+4;
		}
		fishes.addElement(aFish);
	}

	/**
	  * draws the fishes
	  */
	public void drawFishes(Graphics g)
	{
		Image anImage;
		Fish aFish;
		int i;

		for (i=0; i < fishes.size(); i++)
		{
			aFish = (Fish)fishes.elementAt(i);
			if (aFish.dying>0)				// when dying draw animation
			{
				anImage=(Image)bubImages.elementAt(aFish.dying-1);
				g.drawImage(anImage,aFish.x+(aFish.width-anImage.getWidth(null))/2,aFish.y+(aFish.height-anImage.getHeight(null))/2,this);
			}
			else
			{
				if ( (gameState==GAME_STATE_AFTER_GAME)
				&& (highscore.isHighscore(score)) )	// do something special when highscore was achieved
					g.drawImage(aFish.swimImage,aFish.x,aFish.y+(int)(Math.random()*3)-1,this);
				else
					g.drawImage(aFish.swimImage,aFish.x,aFish.y,this);
			}
		}
	}

	/**
	  * moves the fishes around and
	  * checks if a fish arrived at the other side
	  */
	public void moveFishes()
	{
		Fish aFish;
		int i;

		for (i=0; i < fishes.size(); i++)
		{
			aFish = (Fish)fishes.elementAt(i);
			if (aFish.dying>0)
			{
				aFish.dying++;
				if (aFish.dying>7)				// after 7 frames kill dying fish
				{
					fishes.removeElement(aFish);
					newFish();
				}
			}
			else
			{
				if (gameState==GAME_STATE_IN_GAME)
					aFish.x=aFish.x+(aFish.speed+score/1000)*aFish.direction; // speedup every 1000 points
				else
					aFish.x=aFish.x+(aFish.speed)*aFish.direction; // speedup every 1000 points

				if ((aFish.x > getSize().width) || (aFish.x < -aFish.width)) // fish at other side
				{
					fishes.removeElement(aFish);
					newFish();
					if (gameState==GAME_STATE_IN_GAME)		// when we were in game then it's game over now
					{
						if (highscore.isNewHighscore(score))
						{
							gameState=GAME_STATE_AFTER_GAME_HIGHSCORE;
						}
						else gameState=GAME_STATE_AFTER_GAME;
						clickLock=30;
					}
				}
			}
		}
	}

	/**
	  * checks if a mouse click hit a fish and
	  * changes score then
	  */
	public void checkFishKill()
	{
		Fish aFish;
		int i;
		boolean hit=false;

		if (mouseClickButton==1)
		{
			for (i=fishes.size()-1; i>=0; i--)
			{
				aFish = (Fish)fishes.elementAt(i);
				if ( (aFish.isHit(mouseClickX,mouseClickY)) )
				{
					score=score+aFish.speed*10;
					hit=true;
					aFish.kill();
					break;
				}
			}
		}

		if ((mouseClickButton==2) && (dynamiteCount>0))
		{
			for (i=fishes.size()-1; i>=0; i--)
			{
				aFish = (Fish)fishes.elementAt(i);
				score=score+aFish.speed*10;
				aFish.kill();
			}
			hit=true;
			dynamiteCount--;
		}
// uncomment for "trigger happy" penalty
//  if (!hit)
//    score=score-10;
	}


	/**
	  * MouseAdapter for receiving the MouseEvent's we need
	  */
	class FishWorldMouseAdapter extends MouseAdapter
	{
		public void mousePressed(MouseEvent e)
		{
			int modifiers;
			if (clickLock==0)				// only when click is allowed
			{
				mouseClickX=e.getX();
				mouseClickY=e.getY();
				mouseClicked=true;
				if (e.isPopupTrigger()) mouseClickButton=2;
				else mouseClickButton=1;
			}
		}
		public void mouseEntered(MouseEvent e)
		{
			showStatus(getAppletInfo());			// show info in status bar
		}
		public void mouseExited(MouseEvent e)
		{
			showStatus("");
		}
	} // class FishWorldMouseAdapter

	/**
	  * KeyAdapter for receiving the KeyEvent's we need
	  */
	class FishWorldKeyAdapter extends KeyAdapter
	{
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode()==e.VK_BACK_SPACE)
			{
				keyPressedChar='«';
				keyPressed=true;
			}
			else
				if (e.getKeyCode()==e.VK_ENTER)
				{
					keyPressedChar='÷';
					keyPressed=true;
				}
				else
				{
					if ((e.getKeyCode()>=e.VK_COMMA) && (e.getKeyCode()<=e.VK_DIVIDE))
					{
							keyPressedChar=e.getKeyChar();
							keyPressed=true;
					}
				}
		}
	} // class FishWorldKeyAdapter


} // class FishWorld


