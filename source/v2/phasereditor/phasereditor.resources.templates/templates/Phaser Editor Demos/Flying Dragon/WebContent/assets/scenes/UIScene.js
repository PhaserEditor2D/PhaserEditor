
// You can write more code here

/* START OF COMPILED CODE */

class UIScene extends Phaser.Scene {
	
	constructor() {
	
		super('UIScene');
		
	}
	
	_create() {
	
		var up = this.add.image(640.5179, 378.75363, 'Objects', 'Button Up');
		up.setScale(0.5, 0.5);
		
		var flame = this.add.image(80.0, 378.75363, 'Objects', 'Button Flame');
		flame.setScale(0.40251637, 0.3830197);
		
		var down = this.add.image(739.939, 378.75363, 'Objects', 'Button Down');
		down.setScale(0.5, 0.5);
		
		this.fUp = up;
		this.fFlame = flame;
		this.fDown = down;
		
	}
	
	/* START-USER-CODE */

	create() {
		this._create();
		
		this.fFlame.setInteractive();
		this.fUp.setInteractive();
		this.fDown.setInteractive();
		
		this.fFlame._clickEventName = "flameButtonClicked";
		this.fUp._clickEventName = "upButtonClicked";
		this.fDown._clickEventName = "downButtonClicked";
		
		this.input.on("gameobjectup", function (pointer, gameObject) {
			this.emitEvent(gameObject);
		}, this);
		
		
		this.input.keyboard.on("keyup_UP", function () {
			this.emitEvent(this.fUp);
		}, this);
		
		this.input.keyboard.on("keyup_DOWN", function () {
			this.emitEvent(this.fDown);
		}, this);
		
		this.input.keyboard.on("keyup_SPACE", function () {
			this.emitEvent(this.fFlame);
		}, this);
	}
	
	emitEvent(button) {
		var event = button._clickEventName;
		var eventHandler = this.eventListener[event]; 
		if (eventHandler) {
			eventHandler.call(this.eventListener);
		}
	}
	
	init(data) {
		this.eventListener = data.eventListener;
	}

	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
