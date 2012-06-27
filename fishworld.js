/***************************************************************************
 fishworld.js
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


/**
 * FishWorld, a point and click game with fishes <br>
 * Copyright (c) 2001,2002 by Stephan Uhlmann <a href="mailto:su@su2.info">&lt;su@su2.info&gt;</a> <br>
 * 2011 ported to JavaScript/Canvas by Sandy Meier <smeier@kdevelop.org>
 * published under the GPL
 */

var FishWorld = {
    FPS:20,// frames per second
	GAME_STATE_LOADING: 0,
	GAME_STATE_BEFORE_GAME: 1,
	GAME_STATE_IN_GAME: 2,
	GAME_STATE_AFTER_GAME_HIGHSCORE: 3,
	GAME_STATE_AFTER_GAME: 4,

    KEYCODE_A:65,
    KEYCODE_Z:90,
    KEYCODE_BACKSPACE:8,
    KEYCODE_SPACE:32,
    KEYCODE_ENTER:13,

	dbImage: null,					// double buffer image
    graphics: null,
	dbGraphics: null,				// double buffer graphics
	myFgColor:null,
    myBgColor:null,			// foreground and background color
	myFont: null,					// my font
    myFontSize:24,
	mouseClicked:false,				// true when mouse was clicked
	mouseClickX:0,
    mouseClickY:0,			// click position
	mouseClickButton:1,				// button clicked (1=left, 2=right)
	clickLock:0,					// don't allow click after game over for some time
	keyPressed:false,				// true when a key was pressed
	keyPressedChar:"",				// the last key pressed as a char
	fishes:null,					// all fishes currently alive (or just dying), Vector<Fish>
	fishImages: null,			// the fish images even=left, odd=right, Vector<Image>
	bubImages:null,				// the bubble animation, Vector<Image>
	bg01:null,bg02:null,bg03:null,bg04:null,bg11:null,bg12:null,bg13:null,bg14:null,	// 1=before, 0=behind the fishes
	dynamiteImage:null,				// the dynamite
	dynamiteCount:0,				// counter of dynamite
	score:0,					// the current score
	highscore:null,				// highscore table
	name:"",					// name of the player
	gameState:null,					// see GAME_STATE_*
	loadProgress:0,				// load progress (0..27)


	/**
	  * returns info about this applet
	  */
	getAppletInfo: function()
	{
		return "FishWorld 1.2, (c) 2001,2002 by Stephan Uhlmann <su@su2.info>, licensed under the GPL";
	},

	/**
	  * initializes the applet
	  */
    init: function(){
        var me=this;
        var gradient;
        with(this){
            gameState=GAME_STATE_LOADING;
            dbGraphics = document.getElementById("canvasDB").getContext('2d');
            //graphics = document.getElementById("canvas").getContext('2d');
            dbGraphics.font ="bold "+this.myFontSize+"px courier";             
            myFgColor = "rgb(64,64,255)";
            gradient= dbGraphics.createLinearGradient(dbGraphics.canvas.width/2,0, dbGraphics.canvas.width/2,dbGraphics.canvas.height);
            gradient.addColorStop(0,   'rgb(0,0,200)');
            gradient.addColorStop(1,   'rgb(0,0,128)');
            myBgColor = gradient;

            mouseClicked=false;
            clickLock=0;
            addMouseListener();
            keyPressed=false;
            addKeyListener();
            highscore=new Highscore();
            highscore.init();
            name="";
        }
        // graphics are loaded later in run() to display progress bar
                
        this.start();

        //call run every 50ms
        this.loadGraphics();				// load images
        setInterval(function(){
            me.run.call(me);
        },1000/this.FPS);
    },

    /**
     * starts applet by forking a new Thread of ourself and starting this Thread
	  */
	start: function(){					// implemented from Applet
		this.showStatus(this.getAppletInfo());			// show info in browser status bar
	},


	/**
	  * runs the Applet Thread, loops infinite until theadQuit is set to true
	  */
	run: function(){
        var i;
        if (this.gameState==this.GAME_STATE_LOADING){		// loading...
            if(this.loadProgress<27) return;
            //this.loadGraphics();				// load images
            this.fishes = new Array();
            for (i=0; i<6; i++) this.newFish();		// create some fishes
            this.gameState=this.GAME_STATE_BEFORE_GAME;	// ready for game
        }

        if ((this.gameState==this.GAME_STATE_BEFORE_GAME)
                || (this.gameState==this.GAME_STATE_AFTER_GAME))	// before or after game
        {
            if (this.clickLock>0) this.clickLock--;
            if ((this.mouseClicked) && (this.clickLock===0))	// when mouse clicked, start game
            {
                this.fishes = new Array();	// create new fishes
                for (i=0; i<6; i++) this.newFish();
                this.score=0;
                this.dynamiteCount=3;
                this.gameState=this.GAME_STATE_IN_GAME;	// now in game
                this.mouseClicked=false;
            }
            this.moveFishes();			// move the fishes around
            this.repaint();			// and paint everything again
        }

        if (this.gameState==this.GAME_STATE_IN_GAME)	// in game
        {
            if (this.mouseClicked){
                this.checkFishKill();	// check if we hit a fish
                this.mouseClicked=false;
            }
            this.moveFishes();			// move the fishes around
            this.repaint();			// and paint everything again
        }

        if (this.gameState==this.GAME_STATE_AFTER_GAME_HIGHSCORE){	// highscore dialog
            if(this.keyPressed){
                if(this.keyPressedChar!=this.KEYCODE_ENTER){
                    if ((this.keyPressedChar!=this.KEYCODE_BACKSPACE) && (this.name.length<8)){
                        this.name=this.name + String.fromCharCode(this.keyPressedChar);
                    }
                    if ((this.keyPressedChar==this.KEYCODE_BACKSPACE) && (this.name.length>0)){
                        this.name=this.name.substring(0,this.name.length-1);
                    }
                    this.keyPressed=false;
                }else{ // return/enter pressed
                    this.highscore.addHighscore(this.score, this.name);
                    this.gameState=this.GAME_STATE_AFTER_GAME;
                }
            }
            this.moveFishes();	// move the fishes around
            this.repaint();                                    
        }
    },
    
    repaint: function(){
        this.update(this.graphics);
    },
    /**
     * paints the Applet depending from current gameState,
     * uses double buffering
     */
    update: function(g) {
        var s,s2;
        var w,h,w2,h2,i,y;

        w=this.getSize().width; w2=w/2;
        h=this.getSize().height; h2=h/2;

        this.dbGraphics.fillStyle = this.myBgColor;
        this.dbGraphics.fillRect(0,0,w,h);			// clearscreen
        this.dbGraphics.fillStyle = this.myFgColor;

        if (this.gameState==this.GAME_STATE_LOADING)		// while loading...
        {
            s = "Loading graphics...";
            this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,h2-this.myFontSize/2);
            // 27 is the max. loadProgress - 1
            this.dbGraphics.fillRect(w2/2,h2+10,this.loadProgress*(w2/2/27),20);	// progress bar left
            this.dbGraphics.fillRect(w2/2*3-this.loadProgress*(w2/2/27),h2+10,this.loadProgress*(w2/2/27),20);	// progress bar right
        }

        if (this.gameState>this.GAME_STATE_LOADING)		// only draw when finished loading
        // (avoids null pointer exceptions for images)
        {
            // background behind fishes
            this.dbGraphics.drawImage(this.bg01,w2-190,h-118);
            this.dbGraphics.drawImage(this.bg02,w2-234,h-149);
            this.dbGraphics.drawImage(this.bg03,w2+151,h-87);
            this.dbGraphics.drawImage(this.bg04,w2+210,h-32);

            // the fishes
            this.drawFishes(this.dbGraphics);

            // background before fishes
            this.dbGraphics.drawImage(this.bg11,w2-260,h-82);
            this.dbGraphics.drawImage(this.bg12,w2-208,h-119);
            this.dbGraphics.drawImage(this.bg13,w2-160,h-32);
            this.dbGraphics.drawImage(this.bg14,w2-76,h-103);

            if (this.gameState==this.GAME_STATE_BEFORE_GAME){	// before game
                s = "Click to start game";
                this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,h2-this.myFontSize/2);
            }

            if (this.gameState==this.GAME_STATE_IN_GAME)		// in game
            {
                s = "" + this.score;
                this.dbGraphics.fillText(s,w-this.dbGraphics.measureText(s).width,this.myFontSize); // this.score left
                for (i=0;i<this.dynamiteCount;i++){
                    this.dbGraphics.drawImage(this.dynamiteImage,30*i,5); // dynamite left
                }
            }

            if (this.gameState==this.GAME_STATE_AFTER_GAME_HIGHSCORE)	// highthis.score dialog
            {
                s = "" + this.score;
                this.dbGraphics.fillText(s,w-this.dbGraphics.measureText(s).width,this.myFontSize);

                y=this.myFontSize*2;
                s = "New Highscore!";
                this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,y);
                y=y+this.myFontSize*2;
                s = "Enter your name";
                this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,y);
                y=y+this.myFontSize;
                s = this.name + "_";
                this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,y);
            }

            if (this.gameState==this.GAME_STATE_AFTER_GAME)	// after game
            {
                y=this.myFontSize*2;

                s = "Your Highscore: " + this.score;
                this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,y);
                y=y+this.myFontSize;

                s = "Click for a new Game";
                this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,y);
                y=y+this.myFontSize;

                y=y+this.myFontSize;

                for (i=0;i<=9;i++){
                    s2 = "" + (i+1);
                    while (s2.length<2) s2=" "+s2;
                    s=s2+". ";

                    s2 = this.highscore.getName(i);
                    while (s2.length<8) s2=s2+" ";
                    s=s+s2+"  ";

                    s2 = ""+ this.highscore.getPoints(i);
                    while (s2.length<5) s2=" "+s2;
                    s=s+s2;

                    this.dbGraphics.fillText(s,w2-this.dbGraphics.measureText(s).width/2,y);
                    y=y+this.myFontSize;
                }
            }
        } // if (this.gameState>GAME_STATE_LOADING)

       // g.drawImage(dbImage,0,0,this);		// draw double buffer to screen

    },

    /**
     * loads the graphics,
     * increments loadProgress and calls repaint() after every loaded image
     * (to display progress bar)
     */
    loadGraphics: function(){

        var anURL;
        var anImage;
        var i;
        var me=this;
        this.fishImages = new Array();			// the fishes
        this.bubImages = new Array();			// the bubbles

        this.loadProgress=0;
        var imageLoaded = function(){
            me.loadProgress++;
            me.repaint(); // draw progressbar
        };
        this.bg01 = new Image();
        this.bg01.onLoad = imageLoaded();
        this.bg01.src = "graphics/back01.gif";
        this.bg02 = new Image();
        this.bg02.onLoad = imageLoaded();
        this.bg02.src = "graphics/back02.gif";
        this.bg02.onLoad = imageLoaded();
        this.bg03 = new Image();
        this.bg03.src = "graphics/back03.gif";
        this.bg03.onLoad = imageLoaded();
        this.bg04 = new Image();
        this.bg04.src = "graphics/back04.gif";
        this.bg04.onLoad = imageLoaded();
        this.bg11 = new Image();
        this.bg11.src = "graphics/back11.gif";
        this.bg11.onLoad = imageLoaded();
        this.bg12 = new Image();
        this.bg12.src = "graphics/back12.gif";
        this.bg12.onLoad = imageLoaded();
        this.bg13 = new Image();
        this.bg13.src = "graphics/back13.gif";
        this.bg13.onLoad = imageLoaded();
        this.bg14 = new Image();
        this.bg14.src = "graphics/back14.gif";
        this.bg14.onLoad = imageLoaded();
        this.dynamiteImage = new Image();
        this.dynamiteImage.onLoad = imageLoaded();
        this.dynamiteImage.src ="graphics/dynamite.gif";

        // the bubbles
        for (i=0; i<=6; i++){
            anImage = new Image();
            anImage.onLoad = imageLoaded();
            anImage.src = "graphics/exp"+i+".gif";
            this.bubImages.push(anImage);
        }
        // the fish images, from left and from right
        for (i=1; i<=6; i++){
            anImage = new Image();
            anImage.onLoad = imageLoaded();
            anImage.src = "graphics/fish"+i+"l.gif";
            this.fishImages.push(anImage);
            anImage = new Image();
            anImage.onLoad = imageLoaded();
            anImage.src = "graphics/fish"+i+"r.gif";
            this.fishImages.push(anImage);
        }

    },

    /**
     * creates a new Fish and adds him to the fishes Vector
     */
    newFish: function(){
        var aFish;

        aFish = new Fish();
        aFish.type=parseInt((Math.random()*6)+1);		// type: 1..6
        aFish.direction=parseInt(Math.random()*2)*2-1;	// direction: -1 or 1
        if (aFish.direction==1) {
            aFish.swimImage=this.fishImages[(aFish.type-1)*2];
        }else {
            aFish.swimImage=this.fishImages[(aFish.type-1)*2+1];
        }
        aFish.width=aFish.swimImage.width; aFish.height=aFish.swimImage.height;
        if (aFish.direction==1) aFish.x=-aFish.width;
        else aFish.x = this.getSize().width;
        aFish.y=parseInt(Math.random()*(this.getSize().height-aFish.height));
        switch(aFish.type){				// the different fishes are differently fast
            case 1: aFish.speed=parseInt(Math.random()*2)+4;break;
            case 2: aFish.speed=parseInt(Math.random()*2)+3;break;
            case 3: aFish.speed=parseInt(Math.random()*2)+1;break;
            case 4: aFish.speed=parseInt(Math.random()*2)+3;break;
            case 5: aFish.speed=parseInt(Math.random()*2)+2;break;
            case 6: aFish.speed=parseInt(Math.random()*2)+4;
        }
        this.fishes.push(aFish);
    },
    getSize: function(){
        return {
            width: this.dbGraphics.canvas.width,
            height: this.dbGraphics.canvas.height
        }
    },
    /**
     * draws the fishes
     */
    drawFishes: function(g){
        var anImage;
        var aFish;
        var i;

        for (i=0; i < this.fishes.length; i++){
            aFish = this.fishes[i];
            if (aFish.dying>0)				// when dying draw animation
            {
                anImage=this.bubImages[aFish.dying-1];
                g.drawImage(anImage,aFish.x+(aFish.width-anImage.width)/2,aFish.y+(aFish.height-anImage.height)/2);
            }
            else{
                if ( (this.gameState==this.GAME_STATE_AFTER_GAME)
                        && (this.highscore.isHighscore(this.score)) )	// do something special when highthis.score was achieved
                    g.drawImage(aFish.swimImage,aFish.x,aFish.y+parseInt(Math.random()*3)-1);
                else
                    g.drawImage(aFish.swimImage,aFish.x,aFish.y);
            }
        }
    },

    /**
     * moves the this.fishes around and
     * checks if a fish arrived at the other side
     */
    moveFishes: function(){
        var aFish;
        var i;

        for (i=0; i < this.fishes.length; i++){
            aFish = this.fishes[i];
            if (aFish.dying>0){
                aFish.dying++;
                if (aFish.dying>7){				// after 7 frames kill dying fish
                    this.fishes.splice(i,1);
                    this.newFish();
                }
            }else{
                if (this.gameState==this.GAME_STATE_IN_GAME)
                    aFish.x=aFish.x+(aFish.speed+this.score/1000)*aFish.direction; // speedup every 1000 points
                else
                    aFish.x=aFish.x+(aFish.speed)*aFish.direction; // speedup every 1000 points

                if ((aFish.x > this.getSize().width) || (aFish.x < -aFish.width)){ // fish at other side
                    this.fishes.splice(i,1);
                    this.newFish();
                    if (this.gameState==this.GAME_STATE_IN_GAME)		// when we were in game then it's game over now
                    {
                        if (this.highscore.isNewHighscore(this.score))
                        {
                            this.gameState=this.GAME_STATE_AFTER_GAME_HIGHSCORE;
                        }
                        else this.gameState=this.GAME_STATE_AFTER_GAME;
                        this.clickLock=30;
                    }
                }
            }
        }
    },

    /**
     * checks if a mouse click hit a fish and
     * changes this.score then
     */
    checkFishKill: function(){
        var aFish;
        var i;
        var hit=false;

        if (this.mouseClickButton==1){
            for (i=this.fishes.length-1; i>=0; i--){
                aFish = this.fishes[i];
                if ( (aFish.isHit(this.mouseClickX,this.mouseClickY)) ){
                    this.score=this.score+aFish.speed*10;
                    hit=true;
                    aFish.kill();
                    break;
                }
            }
        }

        if ((this.mouseClickButton==2) && (this.dynamiteCount>0)){
            for (i=this.fishes.length-1; i>=0; i--){
                aFish =this.fishes[i];
                this.score=this.score+aFish.speed*10;
                aFish.kill();
            }
            hit=true;
            this.dynamiteCount--;
        }
        // uncomment for "trigger happy" penalty
        if (!hit){
            this.score=this.score-10;
        }
    },
    showStatus: function(text){
        //console.log(text);
    },
    addMouseListener: function(){
        var me = this;
        this.dbGraphics.canvas.addEventListener("mousedown",function(ev){return me.onMouseDown.call(me,ev)},false);
        //disable contextmenu
        this.dbGraphics.canvas.addEventListener("contextmenu",function(ev){ev.preventDefault();return false},false);
    },
    addKeyListener: function(){
        var me = this;
        document.addEventListener("keydown",function(ev){return me.onKeyDown.call(me,ev)},true);
    },
    /**
     * MouseAdapter for receiving the MouseEvent's we need
     */
    onMouseDown: function(ev){
        var modifiers,x,y;
        ev.preventDefault();
        if (this.clickLock===0){				// only when click is allowed
            if (ev.layerX || ev.layerX === 0) { // Firefox
                x = ev.layerX;
                y = ev.layerY;
            } else if (ev.offsetX || ev.offsetX === 0) { // Opera
                x = ev.offsetX;
                y = ev.offsetY;
            }
            this.mouseClickX=x;
            this.mouseClickY=y;
            this.mouseClicked=true;
            if (ev.button==2){
                this.mouseClickButton=2;
                return false;
            }
            else{
                this.mouseClickButton=1;
                return true;

            }

        }
    },
    /**
     * KeyAdapter for receiving the KeyEvent's we need
     */
    onKeyDown: function(ev){
        ev.preventDefault();
        if (ev.keyCode==this.KEYCODE_BACKSPACE || ev.keyCode==this.KEYCODE_SPACE || ev.keyCode==this.KEYCODE_ENTER){
            this.keyPressedChar=ev.keyCode;
            this.keyPressed=true;
        }
        else if ((ev.keyCode>=this.KEYCODE_A) && (ev.keyCode<=this.KEYCODE_Z)){
                this.keyPressedChar=ev.keyCode;
                this.keyPressed=true;
            }
    }

}; // class FishWorld
