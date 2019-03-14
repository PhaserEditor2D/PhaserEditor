
// You can write more code here

/* START OF COMPILED CODE */

class Level extends Phaser.Scene {
	
	constructor() {
	
		super("Level");
		
	}
	
	_create() {
	
		var bg_clouds = this.add.tileSprite(0.0, 0.0, 640.0, 192.0, "bg-clouds");
		bg_clouds.setOrigin(0.0, 0.0);
		bg_clouds.setScrollFactor(0.0, 0.0);
		
		var bg_mountains = this.add.tileSprite(0.0, -10.0, 640.0, 192.0, "bg-mountains");
		bg_mountains.setOrigin(0.0, 0.0);
		bg_mountains.setScrollFactor(0.07, 0.0);
		
		var bg_trees = this.add.tileSprite(0.0, 20.0, 640.0, 192.0, "bg-trees");
		bg_trees.setOrigin(0.0, 0.0);
		bg_trees.setScrollFactor(0.25, 0.0);
		
		this.add.myTilemap(320.00003, 479.99997, "Main Layer");
		
		var ant_3 = this.add.ant(51.40562, 142.9719, "atlas", "ant-1");
		
		var ant_2 = this.add.ant(387.4056, 798.9719, "atlas", "ant-1");
		
		var ant_1 = this.add.ant(259.4056, 462.9719, "atlas", "ant-1");
		
		var grasshoper_3 = this.add.grasshopper(96.0, 800.0, "atlas", "grasshopper-idle-1");
		
		var grasshoper_2 = this.add.grasshopper(80.0, 336.0, "atlas", "grasshopper-idle-1");
		
		var grasshoper_1 = this.add.grasshopper(304.0, 208.0, "atlas", "grasshopper-idle-1");
		
		var gator_3 = this.add.gator(416.0, 576.0, "atlas", "gator-1");
		gator_3.setData("distance", 40);
		gator_3.setData("horizontal", false);
		gator_3.build();
		
		var gator_2 = this.add.gator(576.0, 368.0, "atlas", "gator-1");
		gator_2.setData("distance", 40);
		gator_2.setData("horizontal", false);
		gator_2.build();
		
		var gator_1 = this.add.gator(560.0, 144.0, "atlas", "gator-1");
		gator_1.setData("distance", 40);
		gator_1.setData("horizontal", false);
		gator_1.build();
		
		var acorn_8 = this.add.acorn(592.0, 736.0, "atlas", "acorn-1");
		
		var acorn_7 = this.add.acorn(224.0, 608.0, "atlas", "acorn-1");
		
		var acorn_6 = this.add.acorn(592.0, 736.0, "atlas", "acorn-1");
		
		var acorn_5 = this.add.acorn(560.0, 336.0, "atlas", "acorn-1");
		
		var acorn_4 = this.add.acorn(304.0, 48.0, "atlas", "acorn-1");
		
		var acorn_3 = this.add.acorn(32.0, 448.0, "atlas", "acorn-1");
		
		var acorn_2 = this.add.acorn(32.0, 576.0, "atlas", "acorn-1");
		
		var acorn_1 = this.add.acorn(32.0, 704.0, "atlas", "acorn-1");
		
		var player = this.add.player(191.34131, 816.1016, "atlas", "player-idle-1");
		
		var leaves_1 = this.add.image(64.0, 800.0, "atlas-props", "leaves");
		leaves_1.setOrigin(0.0, 0.0);
		
		var leaves_2 = this.add.image(-48.0, 768.0, "atlas-props", "leaves");
		leaves_2.setOrigin(0.0, 0.0);
		
		var leaves_3 = this.add.image(400.0, 832.0, "atlas-props", "leaves");
		leaves_3.setOrigin(0.0, 0.0);
		
		var leaves_4 = this.add.image(544.0, 768.0, "atlas-props", "leaves");
		leaves_4.setOrigin(0.0, 0.0);
		
		var leaves_5 = this.add.image(528.0, 848.0, "atlas-props", "leaves");
		leaves_5.setOrigin(0.0, 0.0);
		
		var leaves_6 = this.add.image(432.0, 704.0, "atlas-props", "leaves");
		leaves_6.setOrigin(0.0, 0.0);
		
		var leaves_7 = this.add.image(576.0, 512.0, "atlas-props", "leaves");
		leaves_7.setOrigin(0.0, 0.0);
		
		var leaves_8 = this.add.image(32.0, 80.0, "atlas-props", "leaves");
		leaves_8.setOrigin(0.0, 0.0);
		
		var leaves_9 = this.add.image(48.0, 112.0, "atlas-props", "leaves");
		leaves_9.setOrigin(0.0, 0.0);
		
		var leaves_10 = this.add.image(576.0, 80.0, "atlas-props", "leaves");
		leaves_10.setOrigin(0.0, 0.0);
		
		var leaves_11 = this.add.image(48.0, 0.0, "atlas-props", "leaves");
		leaves_11.setOrigin(0.0, 0.0);
		
		var leaves_12 = this.add.image(192.0, 32.0, "atlas-props", "leaves");
		leaves_12.setOrigin(0.0, 0.0);
		
		var branch_05 = this.add.image(528.0, 528.0, "atlas-props", "branch-05");
		branch_05.flipX = true;
		
		var branch_1 = this.add.image(192.0, 512.0, "atlas-props", "branch-01");
		branch_1.setOrigin(0.0, 0.0);
		
		var branch_2 = this.add.image(512.0, 256.0, "atlas-props", "branch-01");
		branch_2.setOrigin(0.0, 0.0);
		
		var branch_3 = this.add.image(80.0, 112.0, "atlas-props", "branch-01");
		branch_3.setOrigin(0.0, 0.0);
		
		var branch_03 = this.add.image(144.0, 640.0, "atlas-props", "branch-03");
		branch_03.setOrigin(0.0, 0.0);
		
		this.fEnemiesGroup = this.add.group([ gator_1, gator_2, gator_3, grasshoper_1, grasshoper_2, grasshoper_3, ant_1, ant_2, ant_3 ]);
		this.fItemsGroup = this.add.group([ acorn_8, acorn_7, acorn_6, acorn_5, acorn_4, acorn_3, acorn_2, acorn_1 ]);
		
		/** @type {Player} */
		this.fPlayer = player;
		
	}
	
	/* START-USER-CODE */

	create() {

		this._create();

		this.cameras.main.startFollow(this.fPlayer);

		this.physics.add.collider(this.fPlayer, this.layer_collisions);
		this.physics.add.collider(this.fEnemiesGroup, this.layer_collisions);

		this.physics.add.overlap(this.fPlayer, this.fEnemiesGroup, this.checkAgainstEnemies, null, this);
		this.physics.add.overlap(this.fPlayer, this.fItemsGroup, this.collectItem, null, this);

		this.bindKeys();

		this.startAudios();

		this.events.on(Phaser.Scenes.Events.SHUTDOWN, function () {
			this.music.stop();
		}, this);
	}

	update() {
		this.movePlayer();

		this.hurtFlagManager();

		// if end is reached display game over screen

		if (this.fPlayer.y < 5 * 16 && this.fPlayer.x < 13 * 16) {
			this.scene.start("GameOver");
		}
	}

	/**
	 * @param {Player} player
	 * @param {Phaser.GameObjects.Sprite} enemy
	 */
	checkAgainstEnemies(player, enemy) {
		if ((player.y + player.body.height * 0.5 < enemy.y) && player.body.velocity.y > 0) {
			enemy.destroy();
			this.audioKill.play();
			this.spawnEnemyDeath(enemy.x, enemy.y);
			player.body.velocity.y = -200;
		} else {
			this.hurtPlayer();
		}
	}
	
	/**
	 * @param {Player} player
	 * @param {Phaser.GameObjects.Sprite} item
	 */
	collectItem(player, item) {
		item.destroy();
		this.audioItem.play();
	}
	
	spawnEnemyDeath(x, y) {
		var temp = new EnemyDeath(this, x, y);
        this.add.existing(temp);
	}

	hurtPlayer() {
		if (this.hurtFlag) {
			return;
		}

		this.hurtFlag = true;
		this.fPlayer.playHurt();
		this.fPlayer.y -= 5;
		this.fPlayer.body.velocity.y = -150;
		this.fPlayer.body.velocity.x = 22 * (this.fPlayer.flipX ? 1 : -1);
		this.audioHurt.play();
	}

	hurtFlagManager() {
		// reset hurt when touching ground
		if (this.hurtFlag && this.fPlayer.body.onFloor()) {
			this.hurtFlag = false;
		}
	}

	startAudios() {
		// audios 
		this.audioKill = this.sound.add("kill");
		this.audioItem = this.sound.add("item");
		this.audioHurt = this.sound.add("hurt");
		this.audioJump = this.sound.add("jump");
		// music
		this.music = this.sound.add("music");
		this.music.play();
	}

	bindKeys() {

		var cursors = this.input.keyboard.createCursorKeys();

		this.wasd = {
			jump: cursors.space,
			left: cursors.left,
			right: cursors.right,
			duck: cursors.down,
			up: cursors.up
		}

		this.input.keyboard.addCapture(
			Phaser.Input.Keyboard.KeyCodes.SPACEBAR,
			Phaser.Input.Keyboard.KeyCodes.LEFT,
			Phaser.Input.Keyboard.KeyCodes.RIGHT,
			Phaser.Input.Keyboard.KeyCodes.DOWN,
			Phaser.Input.Keyboard.KeyCodes.UP
		);

	}

	movePlayer() {
		if (this.hurtFlag) {
			this.fPlayer.playHurt();
			return;
		}

		if (this.wasd.jump.isDown && this.fPlayer.body.onFloor()) {
			this.fPlayer.body.velocity.y = -250;
			this.audioJump.play();
		}

		var vel = 80;

		if (this.wasd.left.isDown) {

			this.fPlayer.body.velocity.x = -vel;

			this.fPlayer.playMoveAnimation();

			this.fPlayer.flipX = true;

		} else if (this.wasd.right.isDown) {

			this.fPlayer.body.velocity.x = vel;

			this.fPlayer.playMoveAnimation();

			this.fPlayer.flipX = false;

		} else {

			this.fPlayer.body.velocity.x = 0;

			this.fPlayer.playStillAnimation(this.wasd.duck.isDown);

		}
	}

	createTilemap() {
		// tiles
		this.globalMap = this.add.tilemap("map");
		this.globalMap.addTilesetImage("tileset");

		this.layer = this.globalMap.createStaticLayer("Main Layer", "tileset");
		this.cameras.main.setBounds(0, 0, this.layer.width, this.layer.height);
		
		this.layer_collisions = this.globalMap.createStaticLayer("Collisions Layer");

		// collisions
		this.globalMap.setCollision([1]);
		this.layer_collisions.visible = false;
		this.layer_collisions.debug = false;
		// one way collisions
		this.setTopCollisionTiles(2);
	}

	setTopCollisionTiles(tileIndex) {
		var x, y, tile;
		for (x = 0; x < this.globalMap.width; x++) {
			for (y = 1; y < this.globalMap.height; y++) {
				var tile = this.globalMap.getTileAt(x, y);
				if (tile !== null) {
					if (tile.index == tileIndex) {
						tile.setCollision(false, false, true, false);
					}
				}
			}
		}
	}


	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

Phaser.GameObjects.GameObjectFactory.register("myTilemap", function() {
	this.scene.createTilemap();
});

