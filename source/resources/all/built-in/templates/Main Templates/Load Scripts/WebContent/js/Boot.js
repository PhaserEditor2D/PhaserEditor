/**
 * Boot state.
 */
function Boot() {
	Phaser.State.call(this);
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State.prototype);
Boot.prototype = proto;
Boot.prototype.constructor = Boot;

Boot.prototype.preload = function() {
	// Load all the assets and to be used in the preload state.
	// Note that the majority of the assets and scripts are loaded in the
	// preload state.
	this.load.pack("boot", "assets/pack.json");
	this.scale.pageAlignVertically = true;
	this.scale.pageAlignHorizontally = true;
	this.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
};

Boot.prototype.create = function() {
	// Register and start the preload state.
	this.state.add("preload", Preload);
	this.game.state.start("preload");
};