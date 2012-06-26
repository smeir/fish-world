/***************************************************************************
                      fish.js
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


/**
  * FishWorld, a point and click game with fishes <br>
  * Copyright (c) 2001,2002 by Stephan Uhlmann <a href="mailto:su@su2.info">&lt;su@su2.info&gt;</a> <br>
  * 2011 ported to JavaScript/Canvas by Sandy Meier <smeier@kdevelop.org>
  * published under the GPL
  */

Fish = function(){ return{
	type: 1,				// type of fish: 1..6
	direction: 0,				// left or right: -1, 1
	x:0,
    y:0,				// current pos
	width: 0,
    height: 0,			// width, height of fish image
	speed: 0,				// movement speed
	dying: 0,				// 0=alive, >0 dying: and this is the current animation frame
	swimImage: null, 			// normal swim image

	/**
	  * check whether fish is hit by shot at x/y
     * @return boolean
     * @param sx int
     * @param sy int
	  */
	isHit: function(sx, sy){
		if ( ( sx >= this.x) && (sx <= this.x+this.width)
		  && (sy >= this.y) && (sy <= this.y+this.height)
		  && this.dying==0 ){
			return true;
		}
		return false;
	},

	/**
	  * kills the fish by setting it to "dying"
	  */
    kill: function(){
		this.dying=1;
	}

}}; // class Fish


