
// You can write more code here

/* START OF COMPILED CODE */

class Level extends Phaser.Scene {
	
	constructor() {
	
		super('Level');
		
	}
	
	_create() {
	
		var player = this.add.sprite(399.99994, 300.0, 'character', 'Jump Loop_000');
		player.setAngle(-10.210137);
		player.anims.play('character - Jump Loop');
		
		var fruit = this.add.image(371.2782, 266.19022, 'character', 'fruit');
		
		this.fPlayer = player;
		this.fFruit = fruit;
		
	}
	
	/* START-USER-CODE */

	create() {
		this._create();

		var text = this.add.text(
			400,
			500,
			"Welcome Phaser Editor 2D!",
			{
				fontFamily: "Arial",
				fontStyle: "bold"
			}
		);
		text.setOrigin(0.5, 0.5);
	}

	update() {
		this.fFruit.angle += 5;
	}


	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
