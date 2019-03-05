
// You can write more code here

/* START OF COMPILED CODE */

class GameOver extends Phaser.Scene {
	
	constructor() {
	
		super("GameOver");
		
	}
	
	_create() {
	
		var bg_clouds = this.add.tileSprite(0.0, 0.0, 288.0, 192.0, "bg-clouds");
		bg_clouds.setOrigin(0.0, 0.0);
		
		var bg_mountains = this.add.tileSprite(0.0, 0.0, 288.0, 192.0, "bg-mountains");
		bg_mountains.setOrigin(0.0, 0.0);
		
		var bg_trees = this.add.tileSprite(0.0, 0.0, 288.0, 192.0, "bg-trees");
		bg_trees.setOrigin(0.0, 0.0);
		
		this.add.image(144.00002, 75.42618, "game-over");
		
		var press_enter_text = this.add.image(144.0, 145.42445, "press-enter-text");
		
		this.fPress_enter_text = press_enter_text;
		
	}
	
	/* START-USER-CODE */

	create() {
		this._create();
		
		this.time.addEvent({
			loop: true,
			delay: 700,
			callback: function() {
				this.blinkText();
			},
			callbackScope: this
		});
		
		this.input.keyboard.on("keydown_ENTER", function () {
			this.scene.start("TitleScreen");			
		}, this);
	}


	blinkText() {
		this.fPress_enter_text.visible = !this.fPress_enter_text.visible;
	}

	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
