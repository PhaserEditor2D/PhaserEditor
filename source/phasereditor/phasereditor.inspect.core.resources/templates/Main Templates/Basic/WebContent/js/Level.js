/**
 * Level state.
 */
function Level() {
	Phaser.State.call(this);
}

/** @type Phaser.State */
var proto = Object.create(Phaser.State);
Level.prototype = proto;

Level.prototype.create = function() {
	this.addMonkey();
	this.moveMonkey();
};

Level.prototype.addMonkey = function() {
	// add monkey
	this.monkey = this.add.sprite(this.world.centerX, this.world.height - 250,
			"mono");
	this.monkey.anchor.set(0.5, 0.5);

	// listen for a monkey click
	this.monkey.inputEnabled = true;
	this.monkey.events.onInputDown.add(this.hitMonkey, this);
};

Level.prototype.moveMonkey = function() {
	// tween monkey like a yoyo
	var twn = this.add.tween(this.monkey);
	twn.to({
		y : 200
	}, 1000, "Quad.easeInOut", true, 0, Number.MAX_VALUE, true);

	// rotate monkey
	twn = this.add.tween(this.monkey);
	twn.to({
		angle : 360
	}, 2000, "Linear", true, 0, Number.MAX_VALUE);
};

Level.prototype.hitMonkey = function() {
	// stop all monkey's movements
	this.tweens.removeAll();

	// rotate monkey
	var twn = this.add.tween(this.monkey);
	twn.to({
		angle : this.monkey.angle + 360
	}, 1000, "Linear", true);

	// scale monkey
	twn = this.add.tween(this.monkey.scale);
	twn.to({
		x : 0.1,
		y : 0.1
	}, 1000, "Linear", true);

	// when tween completes, quit the game
	twn.onComplete.addOnce(this.quitGame, this);
};

Level.prototype.quitGame = function() {
	this.game.state.start("Menu");
};