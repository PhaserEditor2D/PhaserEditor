window.onload = function () {
	var game = new Phaser.Game({{game.width}}, {{game.height}}, {{game.renderer}}{{game.extra}});
    // Add the States your game has.
    // game.state.add("Boot", Boot);
    // game.state.add("Menu", Menu);
    // game.state.add("Preload", Preload);
    game.state.add("Level", Level);
    game.state.start("Level");
};
