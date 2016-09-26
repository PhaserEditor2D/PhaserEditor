// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('cloud', 'assets/gfx/ball.png');
    this.game.load.image('ground', 'assets/gfx/ground.png');
    this.game.load.spritesheet('cyclops', 'assets/gfx/monster.png', 32, 32);
    this.game.load.spritesheet('explosion', 'assets/gfx/explosion.png', 128, 128);
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Let's make some clouds
    for(var x = -56; x < this.game.width; x += 80) {
        var cloud = this.game.add.image(x, -80, 'cloud');
        cloud.scale.setTo(5, 5); // Make the clouds big
        cloud.tint = 0xcccccc; // Make the clouds dark
        cloud.smoothed = false; // Keeps the sprite pixelated
    }

    // Create some ground
    for(x = 0; x < this.game.width; x += 32) {
        this.game.add.image(x, this.game.height - 32, 'ground');
    }

    // Create a pool of cyclopes
    var MONSTERS = 50;
    this.monsterGroup = this.game.add.group();
    this.monsterGroup.enableBody = true;
    this.monsterGroup.physicsBodyType = Phaser.Physics.ARCADE;
    this.monsterGroup.createMultiple(MONSTERS, 'cyclops', 0);

    // Create a timer for spawning a new monster
    this.monsterTimer = 0;

    // Create a group for explosions
    this.explosionGroup = this.game.add.group();

    // Create a bitmap for the lightning bolt texture
    this.lightningBitmap = this.game.add.bitmapData(200, 1000);

    // Create a sprite to hold the lightning bolt texture
    this.lightning = this.game.add.image(this.game.width/2, 80, this.lightningBitmap);

    // This adds what is called a "fragment shader" to the lightning sprite.
    // See the fragment shader code below for more information.
    // This is an WebGL feature. Because it runs in your web browser, you need
    // a browser that support WebGL for this to work.
    this.lightning.filters = [ this.game.add.filter('Glow') ];

    // Set the anchor point of the sprite to center of the top edge
    // This allows us to position the lightning by simply specifiying the
    // x and y coordinate of where we want the lightning to appear from.
    this.lightning.anchor.setTo(0.5, 0);

    // Create a white rectangle that we'll use to represent the flash
    this.flash = this.game.add.graphics(0, 0);
    this.flash.beginFill(0xffffff, 1);
    this.flash.drawRect(0, 0, this.game.width, this.game.height);
    this.flash.endFill();
    this.flash.alpha = 0;

    // Make the world a bit bigger than the stage so we can shake the camera
    this.game.world.setBounds(-10, -10, this.game.width + 20, this.game.height + 20);

    // Trigger lightning on mouse clicks and taps
    this.game.input.onTap.add(this.zap, this);
};

// The update() method is called every frame
GameState.prototype.update = function() {
    // Spawn a new monster
    this.monsterTimer -= this.game.time.elapsed;
    if (this.monsterTimer <= 0) {
        this.monsterTimer = this.game.rnd.integerInRange(150, 500);
        this.createNewMonster();
    }

    // Kill monsters when they go off screen
    this.monsterGroup.forEachAlive(function(monster) {
        if (monster.x < -64 || monster.y > this.game.height) {
            monster.kill();
        }
    }, this);
};

// Create a lightning bolt
GameState.prototype.zap = function() {
    // Kill monsters within 64 pixels of the strike
    this.monsterGroup.forEachAlive(function(monster) {
        if (this.game.math.distance(
                this.game.input.activePointer.x, this.game.input.activePointer.y,
                monster.x, monster.y) < 64) {
            monster.frame = 1; // Show the "dead" texture
            monster.body.velocity.y = this.game.rnd.integerInRange(-600, -1200);
            monster.body.velocity.x = this.game.rnd.integerInRange(-500, 500);
            monster.body.acceleration.y = 3000;
            monster.angle = 180;

            // Create an explosion
            this.getExplosion(monster.x, monster.y);
        }
    }, this);

    // Rotate the lightning sprite so it goes in the
    // direction of the pointer
    this.lightning.rotation =
        this.game.math.angleBetween(
            this.lightning.x, this.lightning.y,
            this.game.input.activePointer.x, this.game.input.activePointer.y
        ) - Math.PI/2;

    // Calculate the distance from the lightning source to the pointer
    var distance = this.game.math.distance(
            this.lightning.x, this.lightning.y,
            this.game.input.activePointer.x, this.game.input.activePointer.y
        );

    // Create the lightning texture
    this.createLightningTexture(this.lightningBitmap.width/2, 0, 20, 3, false, distance);

    // Make the lightning sprite visible
    this.lightning.alpha = 1;

    // Fade out the lightning sprite using a tween on the alpha property.
    // Check out the "Easing function" examples for more info.
    this.game.add.tween(this.lightning)
        .to({ alpha: 0.5 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 1.0 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 0.5 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 1.0 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 0 }, 250, Phaser.Easing.Cubic.In)
        .start();

    // Create the flash
    this.flash.alpha = 1;
    this.game.add.tween(this.flash)
        .to({ alpha: 0 }, 100, Phaser.Easing.Cubic.In)
        .start();

    // Shake the camera by moving it up and down 5 times really fast
    this.game.camera.y = 0;
    this.game.add.tween(this.game.camera)
        .to({ y: -10 }, 40, Phaser.Easing.Sinusoidal.InOut, false, 0, 5, true)
        .start();
};

GameState.prototype.createNewMonster = function() {
    var monster = this.monsterGroup.getFirstDead(); // Recycle a dead monster
    if (monster) {
        monster.reset(this.game.width + 100, this.game.height - 48); // Position on ground
        monster.revive(); // Set "alive"
        monster.body.velocity.setTo(0, 0); // Stop moving
        monster.body.acceleration.setTo(0, 0); // Stop accelerating
        monster.body.velocity.x = -100; // Move left
        monster.rotation = 0; // Reset rotation
        monster.frame = 0; // Set animation frame to 0
        monster.anchor.setTo(0.5, 0.5); // Center texture
    }
};

// Try to get a used explosion from the explosionGroup.
// If an explosion isn't available, create a new one and add it to the group.
// Setup new explosions so that they animate and kill themselves when the
// animation is complete.
GameState.prototype.getExplosion = function(x, y) {
    // Get the first dead explosion from the explosionGroup
    var explosion = this.explosionGroup.getFirstDead();

    // If there aren't any available, create a new one
    if (explosion === null) {
        explosion = this.game.add.sprite(0, 0, 'explosion');
        explosion.anchor.setTo(0.5, 0.5);

        // Add an animation for the explosion that kills the sprite when the
        // animation is complete. Plays the first frame several times to make the
        // explosion more visible after the screen flash.
        var animation = explosion.animations.add('boom', [0,0,0,0,1,2,3], 60, false);
        animation.killOnComplete = true;

        // Add the explosion sprite to the group
        this.explosionGroup.add(explosion);
    }

    console.log('boom');

    // Revive the explosion (set it's alive property to true)
    // You can also define a onRevived event handler in your explosion objects
    // to do stuff when they are revived.
    explosion.revive();

    // Move the explosion to the given coordinates
    explosion.x = x;
    explosion.y = y;

    // Set rotation of the explosion at random for a little variety
    explosion.angle = this.game.rnd.integerInRange(0, 360);

    // Play the animation
    explosion.animations.play('boom');

    // Return the explosion itself in case we want to do anything else with it
    return explosion;
};

// This function creates a texture that looks like a lightning bolt
GameState.prototype.createLightningTexture = function(x, y, segments, boltWidth, branch, distance) {
    // Get the canvas drawing context for the lightningBitmap
    var ctx = this.lightningBitmap.context;
    var width = this.lightningBitmap.width;
    var height = this.lightningBitmap.height;

    // Our lightning will be made up of several line segments starting at
    // the center of the top edge of the bitmap and ending at the target.

    // Clear the canvas
    if (!branch) ctx.clearRect(0, 0, width, height);

    // Draw each of the segments
    for(var i = 0; i < segments; i++) {
        // Set the lightning color and bolt width
        ctx.strokeStyle = 'rgb(255, 255, 255)';
        ctx.lineWidth = boltWidth;

        ctx.beginPath();
        ctx.moveTo(x, y);

        // Calculate an x offset from the end of the last line segment and
        // keep it within the bounds of the bitmap
        if (branch) {
            // For a branch
            x += this.game.rnd.integerInRange(-10, 10);
        } else {
            // For the main bolt
            x += this.game.rnd.integerInRange(-30, 30);
        }
        if (x <= 10) x = 10;
        if (x >= width-10) x = width-10;

        // Calculate a y offset from the end of the last line segment.
        // When we've reached the target or there are no more segments left,
        // set the y position to the distance to the target. For branches, we
        // don't care if they reach the target so don't set the last coordinate
        // to the target if it's hanging in the air.
        if (branch) {
            // For a branch
            y += this.game.rnd.integerInRange(10, 20);
        } else {
            // For the main bolt
            y += this.game.rnd.integerInRange(20, distance/segments);
        }
        if ((!branch && i == segments - 1) || y > distance) {
            // This causes the bolt to always terminate at the center
            // lightning bolt bounding box at the correct distance to
            // the target. Because of the way the lightning sprite is
            // rotated, this causes this point to be exactly where the
            // player clicked or tapped.
            y = distance;
            if (!branch) x = width/2;
        }

        // Draw the line segment
        ctx.lineTo(x, y);
        ctx.stroke();

        // Quit when we've reached the target
        if (y >= distance) break;

        // Draw a branch 20% of the time off the main bolt only
        if (!branch) {
            if (this.game.math.chanceRoll(20)) {
                // Draws another, thinner, bolt starting from this position
                this.createLightningTexture(x, y, 10, 1, true, distance);
            }
        }
    }

    // This just tells the engine it should update the texture cache
    this.lightningBitmap.dirty = true;
};

// Fragment shaders are small programs that run on the graphics card and alter
// the pixels of a texture. Every framework implements shaders differently but
// the concept is the same. This shader takes the lightning texture and alters
// the pixels so that it appears to be glowing. Shader programming itself is
// beyond the scope of this tutorial.
//
// There are a ton of good resources out there to learn it. Odds are that your
// framework already includes many of the most popular shaders out of the box.
//
// This is an OpenGL/WebGL feature. Because it runs in your web browser
// you need a browser that support WebGL for this to work.
Phaser.Filter.Glow = function (game) {
    Phaser.Filter.call(this, game);

    this.fragmentSrc = [
        "precision lowp float;",
        "varying vec2 vTextureCoord;",
        "varying vec4 vColor;",
        'uniform sampler2D uSampler;',

        'void main() {',
            'vec4 sum = vec4(0);',
            'vec2 texcoord = vTextureCoord;',
            'for(int xx = -4; xx <= 4; xx++) {',
                'for(int yy = -3; yy <= 3; yy++) {',
                    'float dist = sqrt(float(xx*xx) + float(yy*yy));',
                    'float factor = 0.0;',
                    'if (dist == 0.0) {',
                        'factor = 2.0;',
                    '} else {',
                        'factor = 2.0/abs(float(dist));',
                    '}',
                    'sum += texture2D(uSampler, texcoord + vec2(xx, yy) * 0.002) * factor;',
                '}',
            '}',
            'gl_FragColor = sum * 0.025 + texture2D(uSampler, texcoord);',
        '}'
    ];
};

Phaser.Filter.Glow.prototype = Object.create(Phaser.Filter.prototype);
Phaser.Filter.Glow.prototype.constructor = Phaser.Filter.Glow;

// Setup game
var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
