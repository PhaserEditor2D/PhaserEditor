/**
 * Level state.
 */
function Level() {
	Phaser.State.call(this);
	// TODO: generated method.
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State.prototype);
Level.prototype = proto;
Level.prototype.constructor = Level;

Level.prototype.create = function() {
	var sprite;
	sprite = this.add.sprite(this.world.centerX, this.world.centerY, "mono");
	this.add.tween(sprite).to({
		'angle' : 360
	}, 1000, "Back.easeInOut", true, 0, Number.MAX_VALUE, false);
	this.input.onDown.addOnce(function() {
		this.game.state.start("menu");
	}, this);
};

Level.prototype.update = function() {
	// TODO: generated method.
};