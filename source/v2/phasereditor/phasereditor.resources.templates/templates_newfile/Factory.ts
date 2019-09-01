namespace Phaser.GameObjects {
	export interface GameObjectFactory {
		{factoryName}(x: number, y: number, texture: string, frame?: string | number) : {className};
	}
}

class {className} extends Phaser.GameObjects.Sprite {

	constructor(scene: Phaser.Scene, x: number, y: number, texture: string, frame: string | integer = null) {
		super(scene, x, y, texture, frame);
	}

}

Phaser.GameObjects.GameObjectFactory.prototype.{factoryName} = function (x: number, y: number, texture: string, frame: string | integer = null) : {className} {
	const scene = this.scene;

	const sprite = new {className}(scene, x, y, texture, frame);

	scene.sys.displayList.add(sprite);
	scene.sys.updateList.add(sprite);

	return sprite;
};