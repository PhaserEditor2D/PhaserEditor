// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image("block", "assets/gfx/block.png");
    this.game.load.image("person", "assets/gfx/player.png");
    this.game.load.image("ball", "assets/gfx/ball.png");
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Create a bitmap texture for drawing lines
    this.bitmap = this.game.add.bitmapData(this.game.width, this.game.height);
    this.bitmap.context.fillStyle = 'rgb(255, 255, 255)';
    this.bitmap.context.strokeStyle = 'rgb(255, 255, 255)';
    this.game.add.image(0, 0, this.bitmap);

    // Build some walls. These will block line of sight.
    var NUMBER_OF_WALLS = 4;
    this.walls = this.game.add.group();
    var i, x, y;
    for(i = 0; i < NUMBER_OF_WALLS; i++) {
        x = i * this.game.width/NUMBER_OF_WALLS + 50;
        y = this.game.rnd.integerInRange(50, this.game.height - 200);
        this.game.add.image(x, y, 'block', 0, this.walls).scale.setTo(3, 3);
    }

    // Place some people in random locations
    var NUMBER_OF_PEOPLE = 6;
    this.people = this.game.add.group();
    for(i = 0; i < NUMBER_OF_PEOPLE; i++) {
        // Choose a random location on the screen
        x = this.game.rnd.integerInRange(32, this.game.width - 32);
        y = this.game.rnd.integerInRange(32, this.game.height - 32);

        // Create a person
        var person = this.game.add.sprite(x, y, 'person');

        // Set the pivot point of the person to the center of the texture
        person.anchor.setTo(0.5, 0.5);

        // Add the person to the people group
        this.people.add(person);
    }

    // Add the ball
    this.ball = this.game.add.sprite(this.game.width/2, this.game.height/2, 'ball');

    // Set the pivot point of the ball to the center of the texture
    this.ball.anchor.setTo(0.5, 0.5);

    // Simulate a pointer click/tap input at the center of the stage
    // when the example begins running.
    this.game.input.activePointer.x = this.game.width/2;
    this.game.input.activePointer.y = this.game.height/2;
};

// The update() method is called every frame
GameState.prototype.update = function() {
    // Separate any people overlapping walls.
    // This isn't necessary for the algorithm but it looks nicer.
    this.walls.forEach(function(wall) {
        this.people.forEach(function(person) {
            if (person.overlap(wall)) {
                if (wall.width > wall.height) {
                    person.y += 64;
                } else {
                    person.x += 64;
                }
            }
        }, this);
    }, this);

    // Move the ball to the pointer/touch location
    this.ball.x = this.game.input.activePointer.x;
    this.ball.y = this.game.input.activePointer.y;

    // Clear the bitmap where we are drawing our lines
    this.bitmap.context.clearRect(0, 0, this.game.width, this.game.height);

    // Ray casting!
    // Test if each person can see the ball by casting a ray (a line) towards the ball.
    // If the ray intersects any walls before it intersects the ball then the wall
    // is in the way.
    this.people.forEach(function(person) {
        // Define a line that connects the person to the ball
        // This isn't drawn on screen. This is just mathematical representation
        // of a line to make our calculations easier. Unless you want to do a lot
        // of math, make sure you choose an engine that has things like line intersection
        // tests built in, like Phaser does.
        var ray = new Phaser.Line(person.x, person.y, this.ball.x, this.ball.y);

        // Test if any walls intersect the ray
        var intersect = this.getWallIntersection(ray);

        if (intersect) {
            // A wall is blocking this persons vision so change them back to their default color
            person.tint = 0xffffff;
        } else {
            // This person can see the ball so change their color
            person.tint = 0xffaaaa;

            // Draw a line from the ball to the person
            this.bitmap.context.beginPath();
            this.bitmap.context.moveTo(person.x, person.y);
            this.bitmap.context.lineTo(this.ball.x, this.ball.y);
            this.bitmap.context.stroke();
        }
    }, this);

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
