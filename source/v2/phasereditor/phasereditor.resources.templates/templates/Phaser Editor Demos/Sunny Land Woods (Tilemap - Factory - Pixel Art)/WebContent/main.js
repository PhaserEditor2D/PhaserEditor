
window.addEventListener('load', function() {

	var game = new Phaser.Game({
		"title": "Sunny Land Woods",
		"width": 288,
		"height": 192,
		"type": Phaser.CANVAS,
		"backgroundColor": "#000",
		"parent": "game-container",
		"scale": {
			"mode": Phaser.Scale.FIT,
			"autoCenter": Phaser.Scale.CENTER_BOTH
		},
		physics: {
			default: "arcade",
			arcade: {
				debug: false
			}
		},
		pixelArt: true
	});
	game.scene.add("Boot", Boot, true);

});

class Boot extends Phaser.Scene {

	preload() {
		this.load.pack("preload-pack", "assets/preload-pack.json");
	}

	update() {
		this.scene.start("Preload");
	}

}
