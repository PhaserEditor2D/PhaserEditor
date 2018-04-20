
function update() {

}


function preload() {
	this.load.image("mono", "assets/mono.png");
}

function create() {
	this.add.sprite(100, 100, "mono");
}

function update() {

}


var game = new Phaser.Game({
	type: {{game.renderer}},
    width: {{game.width}},
    height: {{game.height}},
    backgroundColor: '#fff',
    scene: {
    	preload: preload,
    	create: create,
    	update: update
    }
});

