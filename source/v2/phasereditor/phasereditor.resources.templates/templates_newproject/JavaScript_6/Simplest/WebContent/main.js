
window.addEventListener('load', function() {

	var game = new Phaser.Game({{config}});
	game.scene.add("Boot", Boot, true);
	
});

class Boot extends Phaser.Scene {

	preload() {
		this.load.pack("section1", "assets/pack.json");
	}

	create() {
		this.scene.start("Level");
	}

}