// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('person', 'assets/gfx/player.png');
    this.game.load.image('light', 'assets/gfx/light.png');
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // The radius of the circle of light
    this.LIGHT_RADIUS = 100;

    // Add random people on the screen
    var NUMBER_OF_PEOPLE = 20;
    for(var i = 0; i < NUMBER_OF_PEOPLE; i++) {
        var x = this.game.rnd.integerInRange(100, this.game.width-100);
        var y = this.game.rnd.integerInRange(100, this.game.height-100);
        this.game.add.image(x, y, 'person');
    }

    // Create the shadow texture
    this.shadowTexture = this.game.add.bitmapData(this.game.width, this.game.height);

    // Create an object that will use the bitmap as a texture
    var lightSprite = this.game.add.image(0, 0, this.shadowTexture);

    // Set the blend mode to MULTIPLY. This will darken the colors of
    // everything below this sprite.
    lightSprite.blendMode = Phaser.blendModes.MULTIPLY;

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

    // Update the shadow texture each frame
    this.updateShadowTexture();
};

GameState.prototype.updateShadowTexture = function() {
    // This function updates the shadow texture (this.shadowTexture).
    // First, it fills the entire texture with a dark shadow color.
    // Then it draws a white circle centered on the pointer position.
    // Because the texture is drawn to the screen using the MULTIPLY
    // blend mode, the dark areas of the texture make all of the colors
    // underneath it darker, while the white area is unaffected.

    // Draw shadow
    this.shadowTexture.context.fillStyle = 'rgb(100, 100, 100)';
    this.shadowTexture.context.fillRect(0, 0, this.game.width, this.game.height);

    // Iterate through each of the lights and draw the glow
    this.lights.forEach(function(light) {
        // Randomly change the radius each frame
        var radius = this.LIGHT_RADIUS + this.game.rnd.integerInRange(1,10);

        // Draw circle of light with a soft edge
        var gradient =
            this.shadowTexture.context.createRadialGradient(
                light.x, light.y,this.LIGHT_RADIUS * 0.75,
                light.x, light.y, radius);
        gradient.addColorStop(0, 'rgba(255, 255, 255, 1.0)');
        gradient.addColorStop(1, 'rgba(255, 255, 255, 0.0)');

        this.shadowTexture.context.beginPath();
        this.shadowTexture.context.fillStyle = gradient;
        this.shadowTexture.context.arc(light.x, light.y, radius, 0, Math.PI*2);
        this.shadowTexture.context.fill();
    }, this);

    // This just tells the engine it should update the texture cache
    this.shadowTexture.dirty = true;
};

// Create torch objects
// Torch constructor
var Torch = function(game, x, y) {
    Phaser.Sprite.call(this, game, x, y, 'light');

    // Set the pivot point for this sprite to the center
    this.anchor.setTo(0.5, 0.5);
};

// Torches are a type of Phaser.Sprite
Torch.prototype = Object.create(Phaser.Sprite.prototype);
Torch.prototype.constructor = Torch;

// Setup game
var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
