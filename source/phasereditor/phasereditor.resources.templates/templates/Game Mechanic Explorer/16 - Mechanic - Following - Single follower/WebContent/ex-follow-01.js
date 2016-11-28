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

    // Create a follower
    this.game.add.existing(
        new Follower(this.game, this.game.width/2, this.game.height/2, this.game.input)
    );

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

    // Define constants that affect motion
    this.MAX_SPEED = 250; // pixels/second
    this.MIN_DISTANCE = 32; // pixels
};

// Followers are a type of Phaser.Sprite
Follower.prototype = Object.create(Phaser.Sprite.prototype);
Follower.prototype.constructor = Follower;

Follower.prototype.update = function() {
    // Calculate distance to target
    var distance = this.game.math.distance(this.x, this.y, this.target.x, this.target.y);

    // If the distance > MIN_DISTANCE then move
    if (distance > this.MIN_DISTANCE) {
        // Calculate the angle to the target
        var rotation = this.game.math.angleBetween(this.x, this.y, this.target.x, this.target.y);

        // Calculate velocity vector based on rotation and this.MAX_SPEED
        this.body.velocity.x = Math.cos(rotation) * this.MAX_SPEED;
        this.body.velocity.y = Math.sin(rotation) * this.MAX_SPEED;
    } else {
        this.body.velocity.setTo(0, 0);
    }
};

var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
