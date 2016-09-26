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
	this.load.pack("level", "assets/pack.json");
	this.scale.pageAlignVertically = true;
	this.scale.pageAlignHorizontally = true;
	this.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
	this.world.setBounds(0, 0, 1232, 272);
	// needed for smooth scrolling
	this.game.renderer.renderSession.roundPixels = true;
};

Level.prototype.preload = function() {
	this.load.pack("level", "assets/pack.json");
};

Level.prototype.create = function() {
	this.physics.arcade.gravity.y = 800;

	var scene = new Scene1(this.game);

	// weapon

	this.weapon = this.add.weapon(1, "items");
	this.weapon.setBulletFrames(8, 10, true);
	this.weapon.bulletKillType = Phaser.Weapon.KILL_CAMERA_BOUNDS;
	this.weapon.bulletSpeed = 400;
	this.weapon.bulletGravity.y = -800;
	this.weapon.trackSprite(scene.fPlayer, 0, 8, true);

	// player

	this.player = scene.fPlayer;
	this.camera.follow(this.player, Phaser.Camera.FOLLOW_PLATFORMER);

	// world

	this.ground = scene.fGround;
	this.lava = scene.fLava;

	// enemies

	this.enemies = scene.fEnemies;
	this.enemies.forEach(function(sprite) {
		sprite.play("walk");
	});

	// init physics

	var immovables = [ this.ground, this.lava, this.enemies ];

	for (var i = 0; i < immovables.length; i++) {
		var g = immovables[i];
		g.setAll("body.immovable", true);
		g.setAll("body.allowGravity", false);
	}

	// cursors

	this.cursors = this.input.keyboard.createCursorKeys();
	this.fireButton = this.input.keyboard.addKey(Phaser.KeyCode.SPACEBAR);
};

Level.prototype.update = function() {

	if (!this.player.alive) {
		return;
	}

	// update player velocity

	this.physics.arcade.collide(this.player, this.ground);

	var vel = 0;

	if (this.cursors.left.isDown) {
		vel = -100;
	} else if (this.cursors.right.isDown) {
		vel = 100;
	}

	this.player.body.velocity.x = vel;

	if (vel != 0) {
		this.player.scale.x = this.math.sign(vel);
	}

	// update player animation

	var standing = this.player.body.touching.down;

	if (standing) {
		if (this.cursors.up.isDown) {
			this.player.body.velocity.y = -200;
		}

		if (vel == 0) {
			this.player.play("stay");
		} else {
			this.player.play("walk");
		}
	} else {
		this.player.play("jump");
	}

	// update weapon

	var scaleX = this.player.scale.x;
	this.weapon.bulletSpeed = scaleX * 400;
	this.weapon.bulletAngleOffset = scaleX < 0 ? 180 : 0;

	if (this.fireButton.isDown) {
		this.weapon.fire();
	}

	// update enemies

	this.enemies.forEach(this.moveEnemy);

	this.physics.arcade.collide(this.player, this.lava, this.die, null, this);

};

Level.prototype.moveEnemy = function(sprite) {

	// use the data set in the scene to move the enemies

	var data = sprite.data;

	if (sprite.x < data.left) {
		sprite.body.velocity.x = 50;
	} else if (sprite.x > data.right) {
		sprite.body.velocity.x = -50;
	}

	if (sprite.body.velocity.x < 0) {
		sprite.scale.x = -1;
	} else if (sprite.body.velocity.x > 0) {
		sprite.scale.x = 1;
	}

};

Level.prototype.die = function() {
	// game over
	this.player.play("die");
	this.player.kill();
	this.player.visible = true;
	this.camera.fade();
	this.time.events.add(500, function() {
		this.game.state.start("level");
	}, this);
};