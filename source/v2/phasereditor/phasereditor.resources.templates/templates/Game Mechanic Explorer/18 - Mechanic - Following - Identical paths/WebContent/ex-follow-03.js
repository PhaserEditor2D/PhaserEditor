// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('player', 'assets/gfx/player.png');
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Create 5 followers, each one following the one ahead of it
    // The first one will follow the mouse pointer
    var NUMBER_OF_FOLLOWERS = 10;
    for(var i = 0; i < NUMBER_OF_FOLLOWERS; i++) {
        var f = this.game.add.existing(
            new Follower(this.game,
                this.game.width/2 + i * 32,
                this.game.height/2,
                f || this.game.input /* the previous follower or pointer */
            )
        );
    }

    // Simulate a pointer click/tap input at the center of the stage
    // when the example begins running.
    this.game.input.x = this.game.width/2;
    this.game.input.y = this.game.height/2;
};

// The update() method is called every frame
GameState.prototype.update = function() {
};

// Follower constructor
var Follower = function(game, x, y, target) {
    Phaser.Sprite.call(this, game, x, y, 'player');

    // Save the target that this Follower will follow
    // The target is any object with x and y properties
    this.target = target;

    // Set the pivot point for this sprite to the center
    this.anchor.setTo(0.5, 0.5);

    // Enable physics on this object
    this.game.physics.enable(this, Phaser.Physics.ARCADE);

    // Each Follower will record its position history in
    // an array of point objects (objects with x,y members)
    // This will be used to make each Follower follow the
    // same track as its target
    this.history = [];
    this.HISTORY_LENGTH = 5;

    // Define constants that affect motion
    this.MAX_SPEED = 250; // pixels/second
    this.MIN_DISTANCE = 32; // pixels
};

// Followers are a type of Phaser.Sprite
Follower.prototype = Object.create(Phaser.Sprite.prototype);
Follower.prototype.constructor = Follower;

Follower.prototype.update = function() {
    // Get the target x and y position.
    //
    // This algorithm will follow targets that may or may not have a position
    // history.
    //
    // The targetMoving flag tells this object when its target is moving
    // so that it knows when to move and when to stop.
    var t = {};
    var targetMoving = false;
    if (this.target.history !== undefined && this.target.history.length) {
        // This target has a history so go towards that
        t = this.target.history[0];
        if (this.target.body.velocity.x !== 0 ||
            this.target.body.velocity.y !== 0) targetMoving = true;
    } else {
        // This target doesn't have a history defined so just
        // follow its current x and y position
        t.x = this.target.x;
        t.y = this.target.y;

        // Calculate distance to target
        // If the position is far enough way then consider it "moving"
        // so that we can get this Follower to move.
        var distance = this.game.math.distance(this.x, this.y, t.x, t.y);
        if (distance > this.MIN_DISTANCE) targetMoving = true;
    }

    // If the distance > MIN_DISTANCE then move
    if (targetMoving) {
        // Add current position to the end of the history array
        this.history.push({ x: this.x, y: this.y });

        // If the length of the history array is over a certain size
        // then remove the oldest (first) element
        if (this.history.length > this.HISTORY_LENGTH) this.history.shift();

        // Calculate the angle to the target
        var rotation = this.game.math.angleBetween(this.x, this.y, t.x, t.y);

        // Calculate velocity vector based on rotation and this.MAX_SPEED
        this.body.velocity.x = Math.cos(rotation) * this.MAX_SPEED;
        this.body.velocity.y = Math.sin(rotation) * this.MAX_SPEED;
    } else {
        this.body.velocity.setTo(0, 0);
    }
};

var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
