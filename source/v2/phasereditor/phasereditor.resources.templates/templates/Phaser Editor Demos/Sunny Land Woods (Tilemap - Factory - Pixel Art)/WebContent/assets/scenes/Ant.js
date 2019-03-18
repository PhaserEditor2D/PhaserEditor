/**
 *
 */
class Ant extends Phaser.GameObjects.Sprite {

	constructor(scene, x, y, texture, frame) {
		super(scene, x, y, texture, frame);

		this.play("ant", true);

		this.scene.physics.add.existing(this);
		this.body.setSize(19, 14);
		this.body.setOffset(13, 17);
		this.body.gravity.y = 500;
		this.speed = 40;
		this.body.velocity.x = this.speed;
		this.body.bounce.x = 1;

		this.kind = "ant";
	}

	preUpdate(time, delta) {
		super.preUpdate(time, delta);
		
		this.flipX = this.body.velocity.x > 0;
	}
}

Phaser.GameObjects.GameObjectFactory.register("ant", function(x, y, texture, frame) {
	var sprite = new Ant(this.scene, x, y, texture, frame);

	this.scene.sys.displayList.add(sprite);
	this.scene.sys.updateList.add(sprite);

	return sprite;
});

