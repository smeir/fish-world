/***************************************************************************
                      Highscore.java
                    ------------------
    description          : class representing a highscore table
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
  * published under the GPL
  */

class Highscore
{
	int points[];
	String names[];

	/** consctructor */
	Highscore()
	{
		int i;

		points = new int[10];
		names = new String[10];

		for (i=0;i<=9;i++)
		{
			points[i]=100;
			names[i]="NOBODY";
		}
	}

	/** returns number of points of player no. i */
	int points(int i)
	{
		return points[i];
	}

	/** returns name of player no. i */
	String name(int i)
	{
		return names[i];
	}

	/** checks if score is in Highscore */
	boolean isHighscore(int score)
	{
		return (score>=points[9]);
	}

	/** checks if score is a new Highscore */
	boolean isNewHighscore(int score)
	{
		return (score>points[9]);
	}

	/**
	  * adds score/name to the highscore table,
	  * returns position in highscore table or -1 if no highscore
	  */
	int addHighscore(int score, String name)
	{
		int i,j;

		i=-1;
		if (isNewHighscore(score))			// only when new high score
		{
			for (i=0;i<=9;i++)			// get place in highscore table 
			{
				if (score>points[i]) break;
			}
			for (j=9;j>=i+1;j--)			// move other scores down one place
			{
				points[j]=points[j-1];
				names[j]=names[j-1];
			}
			points[i]=score;			// add score
			names[i]=name;				// add name
		}
		return i;					// return place
	}

} // class Highscore

