// Copyright © 2015 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('ground', 'assets/gfx/ground.png');
    this.game.load.image('ball', 'assets/gfx/ball.png');
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Flag to show or hide the droplets making up the fluid
    this.debug = false;

    // Start the physics system
    this.game.physics.startSystem(Phaser.Physics.P2JS);

    // Add bounciness and gravity
    this.game.physics.p2.restitution = 0.8;
    this.game.physics.p2.gravity.y = 500;

    // Create a new droplet every 50ms
    this.fluid = this.game.add.group();
    var t = this.game.time.create();
    t.loop(50, this.addFluid, this);
    t.start();

    // Add WebGL shaders to "liquify" the droplets
    this.addShaders();

    // Check tap events
    this.game.input.onTap.add(this.tap, this);
};

// Add a single droplet with an initial velocity upwards to simulate a
// fountain.
GameState.prototype.addFluid = function() {
    var randomX = this.game.width/2;
    var randomY = this.game.height * 0.8;

    // Try to recycle a dead droplet from the fluid group first
    var droplet = this.fluid.getFirstDead();

    if (droplet) {
        // A recycled droplet was available, so reset it
        droplet.reset(randomX, randomY);
    } else {
        // No recycled droplets were available so make a new one
        droplet = this.game.add.sprite(randomX, randomY, 'ball');

        // Enable physics for the droplet
        this.game.physics.p2.enable(droplet);
        droplet.body.collideWorldBounds = true;

        // This makes the collision body smaller so that the droplets can get
        // really up close and goopy
        droplet.body.setCircle(droplet.width * 0.3);

        // Add a force that slows down the droplet over time
        droplet.body.damping = 0.3;

        // Add the droplet to the fluid group
        this.fluid.add(droplet);
    }

    // Show/hide the physics body
    droplet.body.debug = this.debug;

    // Initial velocity
    droplet.body.velocity.y = -600;
    droplet.body.velocity.x = this.game.rnd.between(-20, 20);
};

// Add fragment shaders to make the droplets look like a fluid.
GameState.prototype.addShaders = function() {
    var blurShader = this.game.add.filter('Blur');
    blurShader.blur = 32;
    var threshShader = this.game.add.filter('Threshold');
    this.fluid.filters = [ blurShader, threshShader ];
    this.fluid.filterArea = this.game.camera.view;
};

// Show/hide physics bodies when the screen is clicked/tapped
GameState.prototype.tap = function() {
    this.debug = !this.debug;
    this.fluid.setAll('body.debug', this.debug);
};

// The update() method is called every frame
GameState.prototype.update = function() {
    // Move droplets away from the pointer
    if (this.game.input.activePointer.withinGame) {
        this.fluid.forEachAlive(function(droplet) {
            var pointer = this.game.input.activePointer;
            var angle = Math.atan2(pointer.y - droplet.y, pointer.x - droplet.x);
            var distance = Phaser.Point.distance(pointer, droplet);
            if (distance < 80) {
                droplet.body.velocity.x = -Math.cos(angle) * 200;
                droplet.body.velocity.y = -Math.sin(angle) * 200;
            }
        }, this);
    }

    // Remove water that hits the ground
    this.fluid.forEachAlive(function(droplet) {
        if (droplet.y > this.game.height - 15) droplet.kill();
    }, this);
};

// Fragment shaders are small programs that run on the graphics card and alter
// the pixels of a texture. Every framework implements shaders differently but
// the concept is the same. These shaders take the fluid texture and alters
// the pixels so that it appears to be a liquid. Shader programming itself is
// beyond the scope of this tutorial.
//
// There are a ton of good resources out there to learn it. Odds are that your
// framework already includes many of the most popular shaders out of the box.
//
// This is an OpenGL/WebGL feature. Because it runs in your web browser you
// need a browser that support WebGL for this to work.
Phaser.Filter.Threshold = function(game) {
    Phaser.Filter.call(this, game);

    this.fragmentSrc = [
      "precision mediump float;",
      "varying vec2 vTextureCoord;",
      "varying vec4 vColor;",
      "uniform sampler2D uSampler;",

      "void main(void) {",
        "vec4 color = texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y));",
        "float thresh = step(0.3, color.a);",
        "vec4 sum = vec4(thresh * 0.7, thresh * 0.9, thresh, thresh);",
        "gl_FragColor = sum;",

      "}"
    ];

};

Phaser.Filter.Threshold.prototype = Object.create(Phaser.Filter.prototype);
Phaser.Filter.Threshold.prototype.constructor = Phaser.Filter.Threshold;

Phaser.Filter.BlurX = function(game) {
    Phaser.Filter.call(this, game);

    this.uniforms.blur = { type: '1f', value: 1 / 512 };

    this.fragmentSrc = [
      "precision mediump float;",
      "varying vec2 vTextureCoord;",
      "varying vec4 vColor;",
      "uniform float blur;",
      "uniform sampler2D uSampler;",

      "void main(void) {",
        "vec4 sum = vec4(0.0);",

        "sum += texture2D(uSampler, vec2(vTextureCoord.x - 4.0*blur, vTextureCoord.y)) * 0.05;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x - 3.0*blur, vTextureCoord.y)) * 0.09;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x - 2.0*blur, vTextureCoord.y)) * 0.12;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x - blur, vTextureCoord.y)) * 0.15;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y)) * 0.16;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x + blur, vTextureCoord.y)) * 0.15;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x + 2.0*blur, vTextureCoord.y)) * 0.12;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x + 3.0*blur, vTextureCoord.y)) * 0.09;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x + 4.0*blur, vTextureCoord.y)) * 0.05;",

        "gl_FragColor = sum;",

      "}"
    ];

};

Phaser.Filter.BlurX.prototype = Object.create(Phaser.Filter.prototype);
Phaser.Filter.BlurX.prototype.constructor = Phaser.Filter.BlurX;

Object.defineProperty(Phaser.Filter.BlurX.prototype, 'blur', {

    get: function() {
        return this.uniforms.blur.value / (1/7000);
    },

    set: function(value) {
        this.uniforms.blur.value = (1/7000) * value;
    }

});

Phaser.Filter.BlurY = function(game) {
    Phaser.Filter.call(this, game);

    this.uniforms.blur = { type: '1f', value: 1 / 512 };

    this.fragmentSrc = [
      "precision mediump float;",
      "varying vec2 vTextureCoord;",
      "varying vec4 vColor;",
      "uniform float blur;",
      "uniform sampler2D uSampler;",

      "void main(void) {",
        "vec4 sum = vec4(0.0);",

        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y - 4.0*blur)) * 0.05;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y - 3.0*blur)) * 0.09;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y - 2.0*blur)) * 0.12;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y - blur)) * 0.15;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y)) * 0.16;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y + blur)) * 0.15;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y + 2.0*blur)) * 0.12;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y + 3.0*blur)) * 0.09;",
        "sum += texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y + 4.0*blur)) * 0.05;",

        "gl_FragColor = sum;",

      "}"
    ];

};

Phaser.Filter.BlurY.prototype = Object.create(Phaser.Filter.prototype);
Phaser.Filter.BlurY.prototype.constructor = Phaser.Filter.BlurY;

Object.defineProperty(Phaser.Filter.BlurY.prototype, 'blur', {

    get: function() {
        return this.uniforms.blur.value / (1/7000);
    },

    set: function(value) {
        this.uniforms.blur.value = (1/7000) * value;
    }

});

Phaser.Filter.Blur = function(game) {
    this.blurXFilter = new Phaser.Filter.BlurX();
    this.blurYFilter = new Phaser.Filter.BlurY();

    this.passes = [this.blurXFilter, this.blurYFilter];
};

Phaser.Filter.Blur.prototype = Object.create(Phaser.Filter.prototype);
Phaser.Filter.Blur.prototype.constructor = Phaser.Filter.Blur;

Object.defineProperty(Phaser.Filter.Blur.prototype, 'blur', {
    get: function() {
        return this.blurXFilter.blur;
    },
    set: function(value) {
        this.blurXFilter.blur = this.blurYFilter.blur = value;
    }
});

Object.defineProperty(Phaser.Filter.Blur.prototype, 'padding', {
    get: function() {
        return this.blurXFilter.padding;
    },
    set: function(value) {
        this.blurXFilter.padding = this.blurYFilter.padding = value;
    }
});

// Setup game
var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
