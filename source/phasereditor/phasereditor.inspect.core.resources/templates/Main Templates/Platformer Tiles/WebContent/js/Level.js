/**
 * Level state.
 */
function Level() {
	Phaser.State.call(this);
	// TODO: generated method.
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State.prototype);
Level.prototype = proto;
Level.prototype.constructor = Level;

Level.prototype.init = function() {
	this.world.resize(3290, 2275);
	this.world.setBounds(0, 0, this.world.width, this.world.height);
	this.stage.backgroundColor = 0x5e81a1;

	this.physics.startSystem(Phaser.Physics.ARCADE);
	this.physics.arcade.gravity.y = 800;
};

Level.prototype.create = function() {
	var scene1 = new Scene1(this.game);

	this.player = scene1.fPlayer;
	this.playerWalk = this.player.animations.getAnimation("walk");
	this.player.play("stay");

	var immovables = [

	scene1.fGround,

	scene1.fCoins,

	scene1.fMovingPlatforms,

	scene1.fFlyEnemies,

	scene1.fSpikes

	];

	for (var i = 0; i < immovables.length; i += 1) {
		var group = immovables[i];
		group.setAll("body.allowGravity", false);
		group.setAll("body.immovable", true);
	}

	this.ground = scene1.fGround;
	this.coins = scene1.fCoins;
	this.keyYellow = scene1.fKeyYellow;
	this.lockYellow = scene1.fLockYellow;
	this.movingPlatforms = scene1.fMovingPlatforms;

	this.allEnemies = [ scene1.fSpikes, scene1.fFlyEnemies ];
	this.allMoving = [ scene1.fMovingPlatforms, scene1.fFlyEnemies ];

	this.camera.follow(this.player, Phaser.Camera.FOLLOW_PLATFORMER);
	this.cursors = this.input.keyboard.createCursorKeys();

	this.gameOver = false;
};

Level.prototype.update = function() {

	// move sprites

	for (var i = 0; i < this.allMoving.length; i += 1) {
		this.allMoving[i].forEachAlive(this.moveSprite);
	}

	if (this.gameOver) {
		return;
	}

	// player

	this.physics.arcade.collide(this.player, this.ground);
	this.physics.arcade.collide(this.player, this.movingPlatforms);

	var velocity = this.player.body.velocity;
	var scale = this.player.scale;

	if (this.cursors.left.isDown) {
		velocity.x = -200;
		scale.x = -1;
	} else if (this.cursors.right.isDown) {
		velocity.x = 200;
		scale.x = 1;
	} else {
		velocity.x = 0;
	}

	if (velocity.x == 0) {
		this.player.play("stay");
	} else {
		if (!this.player.animations.currentAnim.isPlaying) {
			this.player.play("walk");
		}
	}

	var standing = this.player.body.blocked.down
			|| this.player.body.touching.down;

	if (standing) {
		if (this.cursors.up.isDown) {
			velocity.y = -500;
			this.player.animations.stop();
		} else {
			var walking = this.playerWalk.isPlaying;
			var moving = velocity.x !== 0;

			if (moving) {
				if (!walking) {
					this.player.play("walk");
				}
			} else {
				if (walking) {
					this.player.animations.stop();
				}
				this.player.play("stay");
			}
		}
	} else {
		this.player.play("jump");
	}

	// coins
	this.physics.arcade.overlap(this.player, this.coins, this.playerVsCoins,
			null, this);

	// keys
	this.physics.arcade.overlap(this.player, this.keyYellow, this.playerVsKeys,
			null, this);

	// level lock
	this.physics.arcade.overlap(this.player, this.lockYellow,
			this.playerVsLocks, null, this);

	// enemies
	for (var i = 0; i < this.allEnemies.length; i += 1) {
		this.physics.arcade.overlap(this.player, this.allEnemies[i],
				this.playerVsEnemy, null, this);
	}
};

/**
 * 
 * @param {Phaser.Sprite}
 *            player
 * @param {Phaser.Sprite}
 *            enemy
 */
Level.prototype.playerVsEnemy = function(player, enemy) {
	player.anchor.y = 1;
	player.scale.y = -1;
	player.play("die");
	player.kill();
	player.visible = true;

	this.add.tween(player).to({
		y : player.y - 100
	}, 500, "Circ.easeOut", true, 0);

	this.add.tween(player.scale).to({
		x : 0,
		y : 0
	}, 1500, "Circ.easeOut", true, 0);

	this.time.events.add(1500, function() {
		this.game.state.start("level");
	}, this);

	this.gameOver = true;
};

/**
 * 
 * @param {Phaser.Sprite}
 *            sprite
 */
Level.prototype.moveSprite = function(sprite) {
	// This is an example of how to use the sprite.data property.
	// The data property can be edited in the scene editor.
	var data = sprite.data;
	var vx = data.velocity_x;
	var vy = data.velocity_y;
	var x = sprite.x;
	var y = sprite.y;

	if (vy !== undefined) {
		if ((vy < 0 && y < data.top) || (vy > 0 && y > data.bottom)) {
			data.velocity_y = -vy;
		}
		sprite.body.velocity.y = vy;
	}

	if (vx !== undefined) {
		if ((vx < 0 && x < data.left) || (vx > 0 && x > data.right)) {
			data.velocity_x = -vx;
		}
		sprite.body.velocity.x = vx;
	}

	if (data.face_x !== undefined) {
		if (sprite.body.velocity.x < 0) {
			sprite.scale.x = -1 * data.face_x;
		} else {
			sprite.scale.x = 1 * data.face_x;
		}
	}
};

Level.prototype.playerVsCoins = function(player, coin) {
	// TODO: do something else with the coin
	coin.kill();
};

Level.prototype.playerVsKeys = function(player, key) {
	// TODO: do something else with the key
	key.kill();
};

Level.prototype.playerVsLocks = function(player, lock) {
	if (!this.keyYellow.alive) {
		alert("You win!!!");
		this.game.state.start("level");
	}
};