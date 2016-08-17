window.onload = function() {
	// create the game
	var game = new Phaser.Game(800, 600, Phaser.AUTO);
	// add the level sate
	game.state.add("Level", Level);
	// start the level
	game.state.start("Level");
};
