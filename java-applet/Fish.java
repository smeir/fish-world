/***************************************************************************
                      Fish.java
                    ------------------
    description          : class representing a Fish in the Fishworld game
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

import java.awt.Image;

/**
  * FishWorld, a point and click game with fishes <br>
  * Copyright (c) 2001,2002 by Stephan Uhlmann <a href="mailto:su@su2.info">&lt;su@su2.info&gt;</a> <br>
  * published under the GPL
  */

class Fish
{
	int type;				// type of fish: 1..6
	int direction;				// left or right: -1, 1
	int x, y;				// current pos
	int width, height;			// width, height of fish image
	int speed;				// movement speed
	int dying;				// 0=alive, >0 dying: and this is the current animation frame
	Image swimImage;			// normal swim image

	/**
	  * constructor
	  */
	Fish()
	{
		x=0; y=0; width=0; height=0; speed=0; direction=0; dying=0; swimImage=null;
	}

	/**
	  * check whether fish is hit by shot at x/y
	  */
	boolean isHit(int sx, int sy)
	{
		if ( ( sx >= x) && (sx <= x+width)
		  && (sy >= y) && (sy <= y+height)
		  && dying==0 )
		{
			return true;
		}
		return false;
	}

	/**
	  * kills the fish by setting it to "dying"
	  */
	void kill()
	{
		dying=1;
	}

} // class Fish


