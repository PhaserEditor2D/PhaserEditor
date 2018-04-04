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
    game.add.text(10, 1, "hello world!", { fill: "ff00ff" });
}
function update() {
}
