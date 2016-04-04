// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('ball', 'assets/gfx/ball.png');
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Duration of the movement in milliseconds
    this.MOVE_TIME = 3000;

    // Add the ball
    this.ball = this.game.add.image(0, 0, 'ball');
    this.ball.anchor.setTo(0.5, 0.5);

    // Setup text object to show the name of the active easing function
    this.easingFunctionName = this.game.add.text(
        20, 40, '', { font: '16px Arial', fill: '#ffffff' });

    // Define the tweening functions we'll toggle between
    this.tweenFunctions = [
        { name: "Exponential In", ease: Phaser.Easing.Exponential.In },
        { name: "Exponential Out", ease: Phaser.Easing.Exponential.Out },
        { name: "Exponential InOut", ease: Phaser.Easing.Exponential.InOut }
    ];

    // Setup a canvas to draw the path of the ball on the screen
    this.bitmap = this.game.add.bitmapData(this.game.width, this.game.height);
    this.bitmap.context.fillStyle = 'rgb(255, 255, 255)';
    this.bitmap.context.strokeStyle = 'rgb(255, 255, 255)';
    this.game.add.image(0, 0, this.bitmap);

    // Start the easing function
    this.toggleEasing();

    // Toggle the easing when the mouse button is clicked
    this.game.input.onTap.add(this.toggleEasing, this);
};

GameState.prototype.toggleEasing = function() {
    this.game.tweens.removeAll();

    this.ball.x = 0;
    this.ball.y = 100;

    // Switch to the next easing function
    this.currentEase = (this.currentEase + 1) % this.tweenFunctions.length || 0;
    var easingFunction = this.tweenFunctions[this.currentEase].ease;
    var easingName = this.tweenFunctions[this.currentEase].name;

    // Clear the paths drawn on the screen
    this.bitmap.context.clearRect(0, 0, this.game.width, this.game.height);
    this.bitmap.context.strokeRect(0, 100, this.game.width, this.game.height-200);
    this.bitmap.dirty = true;

    // Display the name of the easing function
    this.easingFunctionName.setText(easingName);

    // Move the ball linearly across the screen
    this.game.add.tween(this.ball).to({ x: this.game.width },
        this.MOVE_TIME, Phaser.Easing.Linear.InOut, true, 0, Number.POSITIVE_INFINITY);

    // Move the ball vertically by the easing function
    this.game.add.tween(this.ball).to({ y: this.game.height - 100 },
        this.MOVE_TIME, easingFunction, true, 0, Number.POSITIVE_INFINITY);
};

// The update() method is called every frame
GameState.prototype.update = function() {
    // Draw the path of the ball on the screen
    this.bitmap.context.fillRect(this.ball.x-1, this.ball.y-1, 2, 2);
    this.bitmap.dirty = true;
};

var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
