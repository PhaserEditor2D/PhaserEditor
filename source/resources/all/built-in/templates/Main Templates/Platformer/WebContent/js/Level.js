/**
 * Level state.
 */
function Level() {
	Phaser.State.call(this);
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State.prototype);
Level.prototype = proto;
Level.prototype.constructor = Level;

Level.prototype.init = function() {
	this.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
	this.scale.pageAlignHorizontally = true;
	this.scale.pageAlignVertically = true;

	this.world.resize(2000, 2000);
	this.world.setBounds(0, 0, 2000, 2000);
	this.physics.startSystem(Phaser.Physics.ARCADE);
	this.physics.arcade.gravity.y = 800;
};

Level.prototype.preload = function() {
	// load all the assets
	this.game.load.pack("section", "assets/pack.json");
};

Level.prototype.create = function() {
	// create the scene
	var scene = new Scene1(this.game);

	// get the player from the scene

	this.player = scene.fDino;
	this.playerWalk = scene.fDino_walk;
	this.playerJump = scene.fDino_jump;
	this.playerStay = scene.fDino_stay;

	// get the platform groups from the scene

	this.platforms = scene.fPlatforms;
	this.movingPlatforms = scene.fMovingPlatforms;

	// init the platforms physics

	this.platforms.setAll("body.allowGravity", false);
	this.platforms.setAll("body.immovable", true);
	this.movingPlatforms.setAll("body.allowGravity", false);
	this.movingPlatforms.setAll("body.immovable", true);
	this.movingPlatforms.setAll("body.velocity.x", -100);

	// init the camera and cursors

	this.camera.follow(this.player, Phaser.Camera.FOLLOW_PLATFORMER);

	this.cursors = this.input.keyboard.createCursorKeys();
};

Level.prototype.update = function() {

	// player and platforms collision

	this.physics.arcade.collide(this.player, this.platforms);
	this.physics.arcade.collide(this.player, this.movingPlatforms);

	// update the moving platforms

	this.movingPlatforms.forEach(this.updatePlatform, this);

	// init the standing var

	var standing = this.player.body.blocked.down
			|| this.player.body.touching.down;

	// update player velocity

	var velocity = this.player.body.velocity;

	velocity.x = 0;

	if (this.cursors.left.isDown) {
		velocity.x = -200;
		this.player.scale.x = -1;
	} else if (this.cursors.right.isDown) {
		velocity.x = 200;
		this.player.scale.x = 1;
	}

	// update player animation
	
	if (standing) {
		if (this.cursors.up.isDown) {
			velocity.y = -800;
		} else {
			var moving = velocity.x !== 0;
			var walking = this.playerWalk.isPlaying;

			if (moving) {
				if (!walking) {
					this.player.play(this.playerWalk.name);
				}
			} else {
				if (walking) {
					this.player.animations.stop();
				}
				this.player.play(this.playerStay.name);
			}
		}
	} else {
		this.player.play(this.playerJump.name);
	}
};

/**
 * Change the platform direction if it touches the world borders.
 * 
 * @param {Phaser.Sprite}
 *            platform
 */
Level.prototype.updatePlatform = function(platform) {
	var velo = platform.body.velocity;
	var right = this.game.world.width - 320;
	if (platform.x < 0 && velo.x < 0) {
		velo.x *= -1;
	} else if (platform.x > right && velo.x > 0) {
		velo.x *= -1;
		platform.x = right;
	}
};
