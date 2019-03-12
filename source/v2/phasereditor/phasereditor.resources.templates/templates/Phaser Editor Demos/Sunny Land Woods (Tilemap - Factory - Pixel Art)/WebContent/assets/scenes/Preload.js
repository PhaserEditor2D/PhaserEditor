
// You can write more code here

/* START OF COMPILED CODE */

class Preload extends Phaser.Scene {
	
	constructor() {
	
		super("Preload");
		
	}
	
	_create() {
	
		var loading = this.add.image(144.0, 96.000015, "loading");
		
		this.fLoading = loading;
		
	}
	
	/* START-USER-CODE */

	preload() {
		this.load.on("progress", function (value) {
			this.fLoading.scaleX = value;
		}, this);
	
		this.load.pack("levels-pack", "assets/levels-pack.json");
		
		this._create();
	}
	
	create() {
		this.scene.start("TitleScreen");
	}

	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
