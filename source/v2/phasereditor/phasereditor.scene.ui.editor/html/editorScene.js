function EditorScene() {
    Phaser.Scene.call(this, "editor");
}

EditorScene.prototype = Object.create(Phaser.Scene.prototype);
EditorScene.prototype.constructor = EditorScene;

EditorScene.prototype.init = function () {
    this.performResize();
};

EditorScene.prototype.performResize = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    scene.scale.resize(window.innerWidth, window.innerHeight);
};

EditorScene.prototype.preload = function () {
    /** @type {Phaser.Loader.LoaderPlugin} */
    var load = this.load;

    load.reset();

    var urls = Models.packs;

    for (var i = 0; i < urls.length; i++) {
        var url = urls[i];

        console.log("Preload: " + url);

        load.setBaseURL(Models.projectUrl);
        load.pack("-asset-pack" + i, url);
    }
};

EditorScene.prototype.create = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    this.initCamera();

    this.initKeyboard();

    this.initSelectionScene();

    EditorCreate.createWorld(scene.add);

};

EditorScene.prototype.initSelectionScene = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    this._toolScene = scene.scene.launch("toolScene");
};

EditorScene.prototype.initKeyboard = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    scene.input.keyboard.on("keydown_I", function () {
        this.cameraZoom(-1);
    }, this);

    scene.input.keyboard.on("keydown_O", function () {
        this.cameraZoom(1);
    }, this);

    scene.input.keyboard.on("keydown_LEFT", function () {
        this.cameraPan(-1, 0);
    }, this);

    scene.input.keyboard.on("keydown_RIGHT", function () {
        this.cameraPan(1, 0);
    }, this);

    scene.input.keyboard.on("keydown_UP", function () {
        this.cameraPan(0, -1);
    }, this);

    scene.input.keyboard.on("keydown_DOWN", function () {
        this.cameraPan(0, 1);
    }, this);
};

EditorScene.prototype.initCamera = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    var cam = scene.cameras.main;

    scene.input.keyboard.addCapture([
        Phaser.Input.Keyboard.KeyCodes.I,
        Phaser.Input.Keyboard.KeyCodes.O,
        Phaser.Input.Keyboard.KeyCodes.LEFT,
        Phaser.Input.Keyboard.KeyCodes.RIGHT,
        Phaser.Input.Keyboard.KeyCodes.UP,
        Phaser.Input.Keyboard.KeyCodes.DOWN,
    ]);
};

EditorScene.prototype.update = function () {
    /** @type {Phaser.Scene} */
    var scene = this;
}

EditorScene.prototype.cameraZoom = function (delta) {
    /** @type {Phaser.Scene} */
    var scene = this;
    var cam = scene.cameras.main;
    if (delta < 0) {
        cam.zoom *= 1.1;
    } else {
        cam.zoom *= 0.9;
    }
}

EditorScene.prototype.cameraPan = function (dx, dy) {
    /** @type {Phaser.Scene} */
    var scene = this;
    var cam = scene.cameras.main;

    cam.scrollX += dx * 30 / cam.zoom;
    cam.scrollY += dy * 30 / cam.zoom;
}

EditorScene.prototype.onMouseWheel = function (e) {
    /** @type {Phaser.Scene} */
    var scene = this;

    var pointer = scene.input.activePointer;
    var cam = scene.cameras.main;
    var delta = e.wheelDelta;

    var zoom = (delta < 0 ? 0.9 : 1.1);

    cam.zoom *= zoom;

}