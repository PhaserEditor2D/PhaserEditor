
function preload() {

}

function create() {
	this.add.text(300, 250, "hello world!", { fill: "#000"});
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




