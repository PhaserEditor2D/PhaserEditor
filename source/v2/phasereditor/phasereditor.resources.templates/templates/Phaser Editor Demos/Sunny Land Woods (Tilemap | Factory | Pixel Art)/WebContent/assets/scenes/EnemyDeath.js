/**
 *
 */
class EnemyDeath extends Phaser.GameObjects.Sprite {

	constructor(scene, x, y, texture, frame) {
		super(scene, x, y, texture, frame);
		this.on(Phaser.Animations.Events.ANIMATION_COMPLETE, this.animationComplete, this);
		this.play("enemy-death");
	}

	animationComplete() {
		this.active = false;
	}

}

