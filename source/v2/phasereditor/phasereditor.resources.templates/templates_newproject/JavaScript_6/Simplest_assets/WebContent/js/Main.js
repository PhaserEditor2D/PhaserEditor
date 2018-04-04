var game = new Phaser.Game(800, 600, Phaser.AUTO, "", this);
function init() {
    game.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
    game.scale.pageAlignHorizontally = true;
    game.scale.pageAlignVertically = true;
}
function preload() {
    game.load.image("mono", "assets/mono.png");
}
function create() {
    game.add.sprite(100, 100, "mono");
}
function update() {
}
