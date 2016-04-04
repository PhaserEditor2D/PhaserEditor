// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('person', 'assets/gfx/player.png');
    this.game.load.image('light', 'assets/gfx/light.png');
    this.game.load.image('blurred-circle', 'assets/gfx/blurred-circle.png');
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Add random people on the screen
    var NUMBER_OF_PEOPLE = 20;
    for(var i = 0; i < NUMBER_OF_PEOPLE; i++) {
        var x = this.game.rnd.integerInRange(100, this.game.width-100);
        var y = this.game.rnd.integerInRange(100, this.game.height-100);
        this.game.add.image(x, y, 'person');
    }

    // Create the lights
    this.lights = this.game.add.group();
    this.lights.add(new Torch(this.game, 200, 150));
    this.lights.add(new Torch(this.game, this.game.width-200, 150));

    this.movingLight = new Torch(this.game, this.game.width/2, this.game.height/2);
    this.lights.add(this.movingLight);

    // Simulate a pointer click/tap input at the center of the stage
    // when the example begins running.
    this.game.input.activePointer.x = this.game.width/2;
    this.game.input.activePointer.y = this.game.height/2;
};

// The update() method is called every frame
GameState.prototype.update = function() {
    // Move the movable light to where the pointer is
    this.movingLight.x = this.game.input.activePointer.x;
    this.movingLight.y = this.game.input.activePointer.y;
};

// Create torch objects
// Torch constructor
var Torch = function(game, x, y) {
    Phaser.Sprite.call(this, game, x, y, 'light');

    // Set the pivot point for this sprite to the center
    this.anchor.setTo(0.5, 0.5);

    // Add a child image that is the glow of the torchlight
    this.glow = this.game.add.image(x, y, 'blurred-circle');
    this.glow.anchor.setTo(0.5, 0.5);

    // Set the blendmode of the glow to ADD. This blendmode
    // has the effect of adding the color of the glow to anything
    // underneath it, brightening it.
    this.glow.blendMode = Phaser.blendModes.ADD;

    // Set the transparency to a low value so decrease the brightness
    this.glow.alpha = 0.5;
};

// Torches are a type of Phaser.Sprite
Torch.prototype = Object.create(Phaser.Sprite.prototype);
Torch.prototype.constructor = Torch;

Torch.prototype.update = function() {
    // Move the glow of this torch to wherever the torch is
    this.glow.x = this.x;
    this.glow.y = this.y;

    // Randomly change the width and height of the glow to simulate flickering
    var size = this.game.rnd.realInRange(0.9, 1.0);
    this.glow.scale.setTo(size, size); // x, y scaling
};

// Setup game
var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
