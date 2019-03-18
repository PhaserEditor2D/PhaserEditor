/**
 *
 */
class Gator extends Phaser.GameObjects.Sprite {

	constructor(scene, x, y, texture, frame) {
		super(scene, x, y, texture, frame);		

		this.play("gator");

		this.scene.physics.add.existing(this);
		this.body.setSize(16, 21);
		this.body.setOffset(15, 20);

		this.initX = this.x;
		this.initY = this.y;

		this.speed = 40;
	}

	build() {
		this.horizontal = this.data.values.horizontal;
		this.distance = this.data.values.distance;
		

		if (this.horizontal) {
			this.body.velocity.x = this.speed;
			this.body.velocity.y = 0;
		} else {
			this.body.velocity.x = 0;
			this.body.velocity.y = this.speed;
		}
	}

	preUpdate(time, delta) {
		super.preUpdate(time, delta);
		
		if (this.horizontal) {
			this.horizontalMove();
		} else {
			this.verticalMove();
		}
	}

	verticalMove() {
		var player = this.getPlayer();

		if (this.body.velocity.y > 0 && this.y > this.initY + this.distance) {
			this.body.velocity.y = -40;
		} else if (this.body.velocity.y < 0 && this.y < this.initY - this.distance) {
			this.body.velocity.y = 40;
		}

		this.flipX = this.x < player.x;

	}

	horizontalMove() {
		var player = this.getPlayer();

		if (this.body.velocity.x > 0 && this.x > this.initX + this.distance) {
			this.body.velocity.x = -40;
		} else if (this.body.velocity.x < 0 && this.x < this.initX - this.distance) {
			this.body.velocity.x = 40;
		}

		this.flipX = this.body.velocity.x > 0;
	}

	/**
	 * @returns {Player}
	 */
	getPlayer() {
		return this.scene.fPlayer;
	}

}

Phaser.GameObjects.GameObjectFactory.register("gator", function(x, y, texture, frame) {
	var sprite = new Gator(this.scene, x, y, texture, frame);

	this.scene.sys.displayList.add(sprite);
	this.scene.sys.updateList.add(sprite);

	return sprite;
});
