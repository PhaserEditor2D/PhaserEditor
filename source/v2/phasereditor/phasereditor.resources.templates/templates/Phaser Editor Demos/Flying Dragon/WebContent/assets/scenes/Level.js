
// You can write more code here

/* START OF COMPILED CODE */

class Level extends Phaser.Scene {
	
	constructor() {
	
		super('Level');
		
	}
	
	_create() {
	
		var sky = this.add.image(0.0, 0.0, 'sky');
		sky.setOrigin(0.0, 0.0);
		
		var clouds_3 = this.add.image(0.0, 163.44876, 'clouds_3');
		clouds_3.setOrigin(0.0, 0.0);
		
		var rocks_2 = this.add.image(0.0, 54.476154, 'rocks_2');
		rocks_2.setOrigin(0.0, 0.0);
		
		var clouds_2 = this.add.image(15.882956, -51.556595, 'clouds_2');
		clouds_2.setOrigin(0.0, 0.0);
		
		var pines = this.add.image(0.0, 0.0, 'pines');
		pines.setOrigin(0.0, 0.0);
		
		var dragon = this.add.sprite(145.22865, 80.51337, 'dragon', 'dragon/Idle_003');
		dragon.anims.play('dragon - dragon/Moving Forward');
		
		var melon_1 = this.add.sprite(87.6739, 718.5431, 'Objects', 'watermelon-1');
		
		var melon_2 = this.add.sprite(167.67389, 718.5431, 'Objects', 'watermelon-1');
		
		var melon_3 = this.add.sprite(247.67389, 718.5431, 'Objects', 'watermelon-1');
		
		var bomb_1 = this.add.sprite(80.0, 660.0, 'Objects', 'bomb-1');
		bomb_1.setOrigin(0.5, 1.0);
		
		var bomb_1_1 = this.add.sprite(160.0, 660.0, 'Objects', 'bomb-1');
		bomb_1_1.setOrigin(0.5, 1.0);
		
		var bomb_1_2 = this.add.sprite(240.0, 660.0, 'Objects', 'bomb-1');
		bomb_1_2.setOrigin(0.5, 1.0);
		
		var fire_1 = this.add.image(435.4351, 621.15967, 'Objects', 'Fire');
		fire_1.setAngle(90.0);
		
		var fire_2 = this.add.image(515.4351, 621.15967, 'Objects', 'Fire');
		fire_2.setAngle(90.0);
		
		var fire_3 = this.add.image(595.4351, 621.15967, 'Objects', 'Fire');
		fire_3.setAngle(90.0);
		
		var clouds_1 = this.add.image(0.0, 156.84879, 'clouds_1');
		clouds_1.setOrigin(0.0, 0.0);
		
		this.fMelons = this.add.group([ melon_3, melon_2, melon_1 ]);
		this.fObjects = this.add.group([ melon_3, melon_2, melon_1, bomb_1, bomb_1_1, bomb_1_2 ]);
		this.fBombs = this.add.group([ bomb_1_2, bomb_1_1, bomb_1 ]);
		this.fFire = this.add.group([ fire_3, fire_2, fire_1 ]);
		this.fParallax = this.add.group([ clouds_1, pines, clouds_2, rocks_2, clouds_3 ]);
		
		this.fDragon = dragon;
		
	}
	
	/* START-USER-CODE */

	create() {
		this._create();

		// UI

		var ui = this.scene.launch("UIScene", {
			eventListener: this
		});
		this.scene.bringToTop("UIScene");

		// Dragon

		this.physics.add.existing(this.fDragon);
		/** @type Phaser.Physics.Arcade.Body */
		this._dragonBody = this.fDragon.body;
		this._dragonBody.setCollideWorldBounds(true);

		// Groups

		for (var sprite of this.fObjects.children.entries) {
			this.initObjectPhysics(sprite);
		}

		for (var sprite of this.fFire.children.entries) {
			this.initObjectPhysics(sprite);
			sprite.body.allowGravity = true;
			sprite.body.gravity.y = -500;
			sprite.body.setSize(30, 50, false);
			sprite.body.setOffset(0, 25);
			sprite.body.enable = false;
		}


		this.physics.add.overlap(this.fFire, this.fObjects, this.Fire_vs_Object, this.notBurning, this);

		this.requestNewObject();

	}

	update() {
		if (!this.fDragon.anims.isPlaying) {
			this.fDragon.anims.play("dragon - dragon/Moving Forward");
		}

		for (var entry of this.fObjects.children.entries) {
			/** @type Phaser.GameObjects.Sprite */
			var sprite = entry;

			if (sprite.active && sprite.x + sprite.width < 0) {
				// out of world
				sprite.active = false;
				sprite.visible = false;
			}

			if (sprite.active && !sprite.visible) {
				sprite.active = false;
			}
		}

		for (var entry of this.fFire.children.entries) {
			/** @type Phaser.GameObjects.Sprite */
			var sprite = entry;

			if (sprite.x - sprite.width > 800) {
				sprite.active = false;
				sprite.visible = false;
			}
		}

		// parallax

		var i = 0;
		var parallax = [4, 3, 2, 1, 0.5]
		for (var obj of this.fParallax.children.entries) {
			/** @type Phaser.GameObjects.Image */
			var sprite = obj;

			sprite.x -= parallax[i];
			if (sprite.x < -sprite.width) {
				sprite.x = 800;
				//sprite.y = Phaser.Math.Between(0, 200);
			}

			i++;
		}

	}

	notBurning(fire, obj) {
		return !obj._burning;
	}

	/**
	 * @param {Phaser.GameObjects.Sprite} fire
	 * @param {Phaser.GameObjects.Sprite} obj
	 */
	Fire_vs_Object(fire, obj) {

		if (this.fBombs.contains(obj)) {
			obj.anims.play("explosion");
			fire.active = false;
			fire.visible = false;
			fire.body.enable = false;
		} else {
			obj.anims.play("Objects - burning-watermelon");
		}

		obj._burning = true;

	}

	/**
	 * @param {Phaser.GameObjects.Sprite} sprite
	 */
	initObjectPhysics(sprite) {
		sprite.active = false;
		sprite.visible = false;

		this.physics.add.existing(sprite);

		/** @type Phaser.Physics.Arcade.StaticBody */
		var body = sprite.body;

		body.allowGravity = false;
	}

	requestNewObject() {
		this.time.addEvent({
			callback: this.spawnObject,
			callbackScope: this,
			delay: Phaser.Math.Between(2000, 3000)
		});
	}

	spawnObject() {
		/** @type Phaser.GameObjects.Sprite */
		var sprite;
		var animKey;

		if (Math.random() <= .5) {
			sprite = this.fBombs.getFirstDead();
			animKey = "Objects - bomb";
		} else {
			sprite = this.fMelons.getFirstDead();
			animKey = "Objects - watermelon";
		}

		if (sprite) {
			sprite.active = true;
			sprite.visible = true;
			sprite._burning = false;

			sprite.body.enbale = true;
			sprite.body.velocity.x = -300;

			sprite.x = 800 + sprite.width - sprite.displayOriginX;
			sprite.y = Phaser.Math.Between(sprite.height, 450 - sprite.height);
			sprite.anims.play(animKey);
		}

		this.requestNewObject();
	}

	flameButtonClicked() {

		/** @type Phaser.GameObjects.Image */
		var fire = this.fFire.getFirstDead();

		if (!fire) {
			return;
		}

		fire.x = this.fDragon.x + this.fDragon.width - this.fDragon.displayOriginX;
		fire.y = this.fDragon.y;
		fire.active = true;
		fire.visible = true;
		/** @type Phaser.Physics.Arcade.StaticBody */
		var fireBody = fire.body;
		fireBody.enable = true;
		fireBody.velocity.x = 500;
		fireBody.velocity.y = 0;
		
		if (this._lastFireUp) {
			fireBody.gravity.y = 400;
			this._lastFireUp = false;
		} else {
			fireBody.gravity.y = -800;
			this._lastFireUp = true;
		}

		var key = this.fDragon.anims.getCurrentKey();
		if (key !== "dragon - dragon/Flaming") {
			this.fDragon.anims.play("dragon - dragon/Flaming");
		}

	}

	upButtonClicked() {
		this.moveDragon(-200);
	}

	downButtonClicked() {
		this.moveDragon(200);
	}

	moveDragon(velocityY) {
		this.fDragon.body.velocity.y = velocityY;
		/*
		if (!this.tweens.isTweening(this.fDragon)) {
			this.tweens.add({
				targets: [this.fDragon],
				duration: 200,
				yoyo: true,
				angle: velocityY > 0? -10 : 10 
			});
		}
		*/
	}

	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
