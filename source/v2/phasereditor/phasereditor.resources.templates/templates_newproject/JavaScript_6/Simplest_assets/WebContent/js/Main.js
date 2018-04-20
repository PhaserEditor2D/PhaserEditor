class GameScene extends Phaser.Scene {
	
	preload() {
		this.load.image("mono", "assets/mono.png");		
	}
			
	create() {
		this.add.sprite(100, 100, "mono");
	}
}

var game = new Phaser.Game({
	type: Phaser.AUTO,
    width: 800,
    height: 600,
    backgroundColor: '#2d2d2d',
    scene: GameScene
}); 