/**
 * Menu state.
 */
function Menu() {
	Phaser.State.call(this);
	// TODO: generated method.
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State.prototype);
Menu.prototype = proto;
Menu.prototype.constructor = Menu;

Menu.prototype.preload = function() {
	var sprite;
	sprite = this.add.sprite(this.world.centerX, this.world.centerY,
			"tap-to-start");
	sprite.anchor.set(0.5);
};

Menu.prototype.create = function() {
	this.input.onDown.addOnce(function() {
		this.game.state.start("level");
	}, this);
};
