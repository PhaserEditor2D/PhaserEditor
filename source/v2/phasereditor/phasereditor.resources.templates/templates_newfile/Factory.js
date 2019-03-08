class {className} extends Phaser.GameObjects.Sprite {

	/**
	 * {className}
	 *
	 * @param {Phaser.Scene} scene
	 * @param x 
	 * @param y 
	 * @param texture
	 * @param frame
	 */
	constructor(scene, x, y, texture, frame) {
		super(scene, x, y, texture, frame);
	}

}

Phaser.GameObjects.GameObjectFactory.register("{factoryName}", function (x, y, texture, frame) {
	
	var sprite = new {className}(this.scene, x, y, texture, frame);

	this.scene.sys.displayList.add(sprite);
	this.scene.sys.updateList.add(sprite);

	return sprite;
});