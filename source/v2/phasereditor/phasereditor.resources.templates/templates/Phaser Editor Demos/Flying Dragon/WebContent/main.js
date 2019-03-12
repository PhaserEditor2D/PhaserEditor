
window.addEventListener('load', function() {

	var game = new Phaser.Game({
		"title": "Flying Dragon (Phaser Editor 2D)",
		"width": 800,
		"height": 450,
		"type": Phaser.AUTO,
		url: "https://phasereditor2d.com",
		parent: "game-container",
		physics: {
			default: "arcade",
			arcade: {
				gravity: {
					y : 400
				},
				debug: false
			}
		},
		scale: {
        	mode: Phaser.Scale.FIT,
        	autoCenter: Phaser.Scale.CENTER_BOTH
    	},
		scene: Boot
	});

});


class Boot extends Phaser.Scene {

	preload() {
		this.load.pack("pack", "assets/pack.json");
	}

	update() {
		this.scene.start("Level");
	}
}

