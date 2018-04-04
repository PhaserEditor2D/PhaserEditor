
var game = new Phaser.Game({{game.width}}, {{game.height}}, {{game.renderer}}, "", this{{game.extra}});

function init() {
	game.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
	game.scale.pageAlignHorizontally = true;
	game.scale.pageAlignVertically = true;
}

function preload() {
	 
}

function create() {
	game.stage.backgroundColor = 0xffffff;
	game.add.text(300, 250, "hello world!", { fill : "#000" });
}

function update() {
	 
}