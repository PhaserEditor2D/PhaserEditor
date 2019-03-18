/**
 *
 */
class Grasshopper extends Phaser.GameObjects.Sprite {

	constructor(scene, x, y, texture, frame) {
		super(scene, x, y, texture, frame);

		this.play("grasshopper-idle");

		this.scene.physics.add.existing(this);
		this.body.setSize(15, 15);
		this.body.setOffset(21, 18);
		this.body.gravity.y = 500;
		this.kind = "Grasshopper";
		this.counter = 0;
		this.jumpCounter = 0;
		this.dirX = 1;
	}

	preUpdate(time, delta) {
		super.preUpdate(time, delta);
		
		// change direction
		if (this.counter++ > 100 && this.body.onFloor()) {
			this.body.velocity.y = -260;
			this.counter = 0;
			// change direction
			if (this.jumpCounter++ > 2) {
				this.jumpCounter = 1;
				this.dirX = this.dirX * -1;
			}
		} else if (this.body.onFloor()) {
			this.body.velocity.x = 0;
			this.play("grasshopper-idle", true);
		} else {
			this.body.velocity.x = 20 * this.dirX;
			if (this.body.velocity.y < 0) {
				this.play("grasshopper-jump", true);
			} else {
				this.play("grasshopper-fall", true);
			}
		}

		//flip
		this.flipX = this.dirX == 1;
	}
}

Phaser.GameObjects.GameObjectFactory.register("grasshopper", function(x, y, texture, frame) {
	var sprite = new Grasshopper(this.scene, x, y, texture, frame);

	this.scene.sys.displayList.add(sprite);
	this.scene.sys.updateList.add(sprite);

	return sprite;
});


