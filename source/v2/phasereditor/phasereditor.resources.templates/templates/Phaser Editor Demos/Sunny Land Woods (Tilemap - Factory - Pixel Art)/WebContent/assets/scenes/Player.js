/**
 *
 */
class Player extends Phaser.GameObjects.Sprite {

	constructor(scene, x, y, texture, frame) {
		super(scene, x, y, texture, frame);

		this.play("player-idle", true);

		this.initX = x;
		this.initY = y;

		this.scene.physics.add.existing(this);

		this.body.setSize(14, 19);
		this.body.setOffset(37, 29);
		
		this.body.gravity.y = 300;
		this.kind = "player";
	}

	playHurt() {
		this.play("player-hurt");
	}

	playMoveAnimation() {
		if (this.body.velocity.y != 0) {
			this.play("player-jump", true);
		} else {
			this.play("player-run", true);
		}
	}

	playStillAnimation(isDucking) {
		if (this.body.velocity.y != 0) {
			this.play("player-jump", true);
		} else if (isDucking) {
			this.play("player-duck", true);
		} else {
			this.play("player-idle", true);
		}
	}
}

Phaser.GameObjects.GameObjectFactory.register("player", function(x, y, texture, frame) {	
	var sprite = new Player(this.scene, x, y, texture, frame);

	this.scene.sys.displayList.add(sprite);
	this.scene.sys.updateList.add(sprite);

	return sprite;
});

