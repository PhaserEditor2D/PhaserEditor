// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image("block", "assets/gfx/block.png");
    this.game.load.image("light", "assets/gfx/light.png");
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Add the light
    this.light = this.game.add.sprite(this.game.width/2, this.game.height/2, 'light');

    // Set the pivot point of the light to the center of the texture
    this.light.anchor.setTo(0.5, 0.5);

    // Create a bitmap texture for drawing light cones
    this.bitmap = this.game.add.bitmapData(this.game.width, this.game.height);
    this.bitmap.context.fillStyle = 'rgb(255, 255, 255)';
    this.bitmap.context.strokeStyle = 'rgb(255, 255, 255)';
    var lightBitmap = this.game.add.image(0, 0, this.bitmap);

    // This bitmap is drawn onto the screen using the MULTIPLY blend mode.
    // Since this bitmap is over the background, dark areas of the bitmap
    // will make the background darker. White areas of the bitmap will allow
    // the normal colors of the background to show through. Blend modes are
    // only supported in WebGL. If your browser doesn't support WebGL then
    // you'll see gray shadows and white light instead of colors and it
    // generally won't look nearly as cool. So use a browser with WebGL.
    lightBitmap.blendMode = Phaser.blendModes.MULTIPLY;

    // Build some walls. These will block line of sight.
    var NUMBER_OF_WALLS = 4;
    this.walls = this.game.add.group();
    var i, x, y;
    for(i = 0; i < NUMBER_OF_WALLS; i++) {
        x = i * this.game.width/NUMBER_OF_WALLS + 50;
        y = this.game.rnd.integerInRange(50, this.game.height - 200);
        this.game.add.image(x, y, 'block', 0, this.walls).scale.setTo(3, 3);
    }

    // Simulate a pointer click/tap input at the center of the stage
    // when the example begins running.
    this.game.input.activePointer.x = this.game.width/2;
    this.game.input.activePointer.y = this.game.height/2;
};

// The update() method is called every frame
GameState.prototype.update = function() {
    // Move the light to the pointer/touch location
    this.light.x = this.game.input.activePointer.x;
    this.light.y = this.game.input.activePointer.y;

    // Next, fill the entire light bitmap with a dark shadow color.
    this.bitmap.context.fillStyle = 'rgb(100, 100, 100)';
    this.bitmap.context.fillRect(0, 0, this.game.width, this.game.height);

    // Ray casting!
    // Cast rays at intervals in a large circle around the light.
    // Save all of the intersection points or ray end points if there was no intersection.
    var points = [];
    for(var a = 0; a < Math.PI * 2; a += Math.PI/360) {
        // Create a ray from the light to a point on the circle
        var ray = new Phaser.Line(this.light.x, this.light.y,
            this.light.x + Math.cos(a) * 1000, this.light.y + Math.sin(a) * 1000);

        // Check if the ray intersected any walls
        var intersect = this.getWallIntersection(ray);

        // Save the intersection or the end of the ray
        if (intersect) {
            points.push(intersect);
        } else {
            points.push(ray.end);
        }
    }

    // Connect the dots and fill in the shape, which are cones of light,
    // with a bright white color. When multiplied with the background,
    // the white color will allow the full color of the background to
    // shine through.
    this.bitmap.context.beginPath();
    this.bitmap.context.fillStyle = 'rgb(255, 255, 255)';
    this.bitmap.context.moveTo(points[0].x, points[0].y);
    for(var i = 0; i < points.length; i++) {
        this.bitmap.context.lineTo(points[i].x, points[i].y);
    }
    this.bitmap.context.closePath();
    this.bitmap.context.fill();

    // This just tells the engine it should update the texture cache
    this.bitmap.dirty = true;
};

// Given a ray, this function iterates through all of the walls and
// returns the closest wall intersection from the start of the ray
// or null if the ray does not intersect any walls.
GameState.prototype.getWallIntersection = function(ray) {
    var distanceToWall = Number.POSITIVE_INFINITY;
    var closestIntersection = null;

    // For each of the walls...
    this.walls.forEach(function(wall) {
        // Create an array of lines that represent the four edges of each wall
        var lines = [
            new Phaser.Line(wall.x, wall.y, wall.x + wall.width, wall.y),
            new Phaser.Line(wall.x, wall.y, wall.x, wall.y + wall.height),
            new Phaser.Line(wall.x + wall.width, wall.y,
                wall.x + wall.width, wall.y + wall.height),
            new Phaser.Line(wall.x, wall.y + wall.height,
                wall.x + wall.width, wall.y + wall.height)
        ];

        // Test each of the edges in this wall against the ray.
        // If the ray intersects any of the edges then the wall must be in the way.
        for(var i = 0; i < lines.length; i++) {
            var intersect = Phaser.Line.intersects(ray, lines[i]);
            if (intersect) {
                // Find the closest intersection
                distance =
                    this.game.math.distance(ray.start.x, ray.start.y, intersect.x, intersect.y);
                if (distance < distanceToWall) {
                    distanceToWall = distance;
                    closestIntersection = intersect;
                }
            }
        }
    }, this);

    return closestIntersection;
};

// Setup game
var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
