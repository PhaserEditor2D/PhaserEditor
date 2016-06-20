
- phaser-master\src\input\Keyboard.js
	- make Phaser.KeyCode a @class and not a @namespace.
	- replace all /** @static **/ to 
		/**
		@type {Number}
		@static
		*/


- phaser-master\src\physics\arcade\Body.js
	- The method onCeiling jsdoc was fixed, it was using #onTop
	
- phaser-master\src\loader\Loader.js
	- Fix jsdoc of audioSprite, it was using #audiosprite
	
- phaser-master\src\geom\Line.js
	- Fix jsdoc of intersectsRectangle (set it to @method Phaser.Line.intersectsRectangle)