/**
 * @param {Phaser.State}
 *            state
 * @param {String}
 *            nextLevel
 * @param {Dino}
 *            dino
 * @param {Door}
 *            door
 * @param {Phaser.Group}
 *            platforms
 * @param {Phaser.Group}
 *            movingPlatforms
 */
function PlatformerBehavior(state, nextLevel, dino, door, platforms,
		movingPlatforms) {
	// init

	this._state = state;
	this._nextLevel = nextLevel;

	// physics
	this._arcade = state.game.physics.arcade;
	this._arcade.gravity.y = 800;

	// dino
	this._dino = dino;
	this._state.camera.follow(this._dino, Phaser.Camera.FOLLOW_PLATFORMER);

	// door
	this._door = door;

	// platforms
	this._platforms = platforms;
	this._arcade.enable(this._platforms, true);
	this._platforms.setAll("body.allowGravity", false);
	this._platforms.setAll("body.immovable", true);

	if (movingPlatforms) {
		this._movingPlatforms = movingPlatforms;
		this._arcade.enable(this._movingPlatforms, true);
		this._movingPlatforms.setAll("body.allowGravity", false);
		this._movingPlatforms.setAll("body.immovable", true);
		this._movingPlatforms.setAll("body.velocity.x", -100);
	}

	// cursors
	this._cursors = this._state.input.keyboard.createCursorKeys();
}

PlatformerBehavior.prototype.update = function() {

	// player and door collision
	this._arcade.overlap(this._dino, this._door, this.startNextLevel, null, this);

	// player and platforms collision

	this._arcade.collide(this._dino, this._platforms);

	if (this._movingPlatforms) {
		this._arcade.collide(this._dino, this._movingPlatforms);
		this._movingPlatforms.forEach(this.updatePlatform, this);
	}

	// init the standing var

	var standing = this._dino.body.blocked.down
			|| this._dino.body.touching.down;

	// update player velocity

	var velocity = this._dino.body.velocity;

	velocity.x = 0;

	if (this._cursors.left.isDown) {
		velocity.x = -200;
		this._dino.scale.x = -1;
	} else if (this._cursors.right.isDown) {
		velocity.x = 200;
		this._dino.scale.x = 1;
	}

	// update player animation

	if (standing) {

		var moving = this._dino.body.velocity.x != 0;

		if (this._cursors.up.isDown) {
			velocity.y = -800;
			this._dino.play("jump");
		} else if (moving) {
			this._dino.play("walk");
		} else {
			this._dino.play("stay");
		}
	} else {
		this._dino.play("jump");
	}
};

PlatformerBehavior.prototype.startNextLevel = function() {
	if (!this._levelOver) {
		this._levelOver = true;
		this._state.camera.fade();
		this._state.time.events.add(1000, function (){
			this._state.game.state.start(this._nextLevel);
		}, this);
	}
};

/**
 * Change the platform direction if it touches the world borders.
 * 
 * @param {Phaser.Sprite}
 *            platform
 */
PlatformerBehavior.prototype.updatePlatform = function(platform) {
	var velo = platform.body.velocity;
	var right = this._state.world.width - 320;
	if (platform.x < 0 && velo.x < 0) {
		velo.x *= -1;
	} else if (platform.x > right && velo.x > 0) {
		velo.x *= -1;
		platform.x = right;
	}
};

PlatformerBehavior.prototype.debug = function() {
	this._state.game.debug.body(this._dino);
	this._platforms.forEach(function(p) {
		this._state.game.debug.body(p);
	}, this);
};
