/**
 * Preload state.
 */
function Preload() {
	Phaser.State.call(this);
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State);
Preload.prototype = proto;

Preload.prototype.preload = function() {
	// This sets the preloadBar sprite as a loader sprite.
	// What that does is automatically crop the sprite from 0 to full-width
	// as the files below are loaded in.
	var preloadBar = this.add.sprite(this.world.centerX, this.world.centerY,
			"loading");
	preloadBar.anchor.set(0.5, 0.5);
	this.load.setPreloadSprite(preloadBar);

	// Here we load the rest of the assets our game needs.
	this.load.pack("start", "assets/assets-pack.json");
	this.load.pack("level", "assets/assets-pack.json");
};

Preload.prototype.create = function() {
	this.game.state.start("Menu");
};