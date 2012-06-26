/***************************************************************************
 highscore.js
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
 * 2011 ported to JavaScript/Canvas by Sandy Meier <smeier@kdevelop.org>
 * published under the GPL
 */

Highscore = function(){ return {
        points: null,
        names: null,

        /** consctructor */
        init: function() {
            var i;

            this.points = new Array();
            this.names = new Array();

            for (i=0;i<=9;i++){
                this.points[i]=100;
                this.names[i]="NOBODY";
            }
        },

        /** returns number of points of player no. i */
        getPoints: function(i){
            return this.points[i];
        },

        /** returns name of player no. i */
        getName: function(i){
            return this.names[i];
        },

        /** checks if score is in Highscore */
        isHighscore: function(score){
            return (score>=this.points[9]);
        },

        /** checks if score is a new Highscore */
        isNewHighscore: function(score){
            return (score>this.points[9]);
        },

        /**
         * adds score/name to the highscore table,
         * returns position in highscore table or -1 if no highscore
         */
        addHighscore: function(/*int*/ score, /*String*/ name)
        {
            var i,j;

            i=-1;
            if (this.isNewHighscore(score)){			// only when new high score
                for (i=0;i<=9;i++){			// get place in highscore table
                    if (score>this.points[i]) break;
                }
                for (j=9;j>=i+1;j--){			// move other scores down one place
                    this.points[j]=this.points[j-1];
                    this.names[j]=this.names[j-1];
                }
                this.points[i]=score;			// add score
                this.names[i]=name;				// add name
            }
            return i;					// return place
        }

    }
}; // class Highscore

