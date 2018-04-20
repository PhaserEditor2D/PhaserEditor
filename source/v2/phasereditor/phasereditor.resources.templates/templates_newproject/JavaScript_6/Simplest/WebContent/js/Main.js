
class GameScene extends Phaser.Scene {
			
	create() {
		this.add.text(10, 1, "hello world!", { fill: "ff00ff" });
	}
}

var game = new Phaser.Game({
	type: Phaser.AUTO,
    width: 800,
    height: 600,
    backgroundColor: '#fff',
    scene: GameScene
}); 