window.onload = function() {
	var game = new Phaser.Game(800, 600, Phaser.AUTO);

	// Add the States your game has.
	game.state.add("Boot", Boot);
	game.state.add("Preloader", Preloader);
	game.state.add("Menu", Menu);
	game.state.add("Level1", Level1);
	game.state.add("Level2", Level2);
	game.state.add("Level3", Level3);

	game.state.start("Boot");
};
