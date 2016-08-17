/**
 * Preloader state.
 */
function Preload() {
	Phaser.State.call(this);
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State.prototype);
Preload.prototype = proto;
Preload.prototype.constructor = Preload;

Preload.prototype.preload = function() {
	// Load the assets of the game, including the scripts.
	this.load.pack("preload", "assets/pack.json");
	var sprite;
	sprite = this.add.image(this.world.centerX, this.world.centerY, "loading");
	sprite.anchor.set(0.5, 0.5);
};

Preload.prototype.create = function() {
	// Add the next states of the game
	this.game.state.add("level", Level);
	this.game.state.start("level");
};
