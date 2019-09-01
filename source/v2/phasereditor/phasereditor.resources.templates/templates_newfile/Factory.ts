namespace Phaser.GameObjects {
	export interface GameObjectFactory() {
		{factoryName}(x, y, texture, frame);
	}
}

class {className} extends Phaser.GameObjects.Sprite {

	constructor(scene: Phaser.Scene, x: number, y: number, texture: string, frame: string) {
		super(scene, x, y, texture, frame);
	}

}

Phaser.GameObjects.GameObjectFactory.register("{factoryName}", function (x, y, texture, frame) {
	const self = Phaser.GameObjects.GameObjectFactory(this);

	var sprite = new {className}(self.scene, x, y, texture, frame);

	self.scene.sys.displayList.add(sprite);
	self.scene.sys.updateList.add(sprite);

	return sprite;
});