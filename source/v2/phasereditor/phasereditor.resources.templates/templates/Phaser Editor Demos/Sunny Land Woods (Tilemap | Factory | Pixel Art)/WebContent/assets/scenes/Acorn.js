Phaser.GameObjects.GameObjectFactory.register("acorn", function (x, y, texture, frame) {

	/** @type {Phaser.Scene} */
	var scene = this.scene;
	
	var sprite = scene.add.sprite(x, y, texture, frame);
	scene.physics.add.existing(sprite);
	sprite.play("acorn");
	
	return sprite;
});

