
// You can write more code here

/* START OF COMPILED CODE */

class TitleScreen extends Phaser.Scene {
	
	constructor() {
	
		super("TitleScreen");
		
	}
	
	_create() {
	
		var bg_clouds = this.add.tileSprite(0.0, 0.0, 288.0, 192.0, "bg-clouds");
		bg_clouds.setOrigin(0.0, 0.0);
		bg_clouds.setScrollFactor(0.0, 0.0);
		
		var bg_mountains = this.add.tileSprite(0.0, 20.0, 288.0, 192.0, "bg-mountains");
		bg_mountains.setOrigin(0.0, 0.0);
		bg_mountains.setScrollFactor(0.07, 0.0);
		
		var bg_trees = this.add.tileSprite(0.0, -10.0, 288.0, 192.0, "bg-trees");
		bg_trees.setOrigin(0.0, 0.0);
		bg_trees.setScrollFactor(0.25, 0.0);
		
		var instructions = this.add.image(144.00002, 75.42618, "instructions");
		instructions.visible = false;
		
		var title = this.add.image(144.00002, 96.0, "title-screen");
		
		this.add.image(144.00002, 170.16515, "credits-text");
		
		var press_enter_text = this.add.image(144.00002, 145.42445, "press-enter-text");
		
		this.fInstructions = instructions;
		this.fTitle = title;
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
		
		this.input.keyboard.on("keydown_ENTER", this.startGame, this);
	}

	blinkText() {
		this.fPress_enter_text.visible = !this.fPress_enter_text.visible;
	}
	
	startGame() {
		if (this.fInstructions.visible) {
			this.scene.start("Level");
		} else {
			this.fTitle.destroy();
			this.fInstructions.visible = true;
		}
	}

	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
