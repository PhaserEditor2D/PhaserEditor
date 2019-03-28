/**
 * 
 * @param {Broker} broker 
 */
function Editor(socket) {

    this._socket = socket;

    var self = this;

    this._socket.onopen = function () {
        self.sendMessage({
            method: "GetRefreshAll"
        });
    };
    this._socket.onmessage = function (event) {
        var msg = JSON.parse(event.data);
        self.messageReceived(msg);
    };



    this._game = new Phaser.Game({
        "title": "Phaser Editor 2D - Web Scene Editor",
        "width": window.innerWidth,
        "height": window.innerWidth,
        "type": Phaser.AUTO,
        url: "https://phasereditor2d.com",
        parent: "editorContainer",
        scale: {
            mode: Phaser.Scale.NONE,
            autoCenter: Phaser.Scale.NO_CENTER
        },
        scene: EditorScene,
        backgroundColor: "#d3d3d3"
    });

    this._resizeToken = 0;


    window.addEventListener('resize', function (event) {
        self._resizeToken += 1;
        setTimeout((function (token) {
            return function () {
                if (token === self._resizeToken) {
                    self._game.scale.resize(window.innerWidth, window.innerHeight - 10);
                }
            };
        })(self._resizeToken), 200);
    }, false);

    window.addEventListener("mousewheel", function (e) {
        self.getScene().onMouseWheel(e);
    });
}

Editor.prototype = Object.create(Object.prototype);
Editor.prototype.constructor = Editor;

Editor.prototype.sendMessage = function (msg) {
    this._socket.send(JSON.stringify(msg));
};

/**
 * @returns {SceneEditor} the SceneEditor
 */
Editor.prototype.getScene = function () {
    return this._game.scene.scenes[0];
};

Editor.prototype.messageReceived = function (batch) {

    console.log(batch);

    var list = batch.list;

    for (var i = 0; i < list.length; i++) {
        var msg = list[i];

        var method = msg.method;

        switch (method) {
            case "RefreshAll":
                this.onRefreshAll(msg);
                break;
        }
    }
};

Editor.prototype.onRefreshAll = function (msg) {
    /** @type {Phaser.Scene} */
    var editorScene = this.getScene();

    Models.displayList = msg.displayList;
    Models.projectUrl = msg.projectUrl;
    Models.packs = msg.packs;

    editorScene.scene.restart();
}



function EditorScene() {
    Phaser.Scene.call(this, "editor");
}

EditorScene.prototype = Object.create(Phaser.Scene.prototype);
EditorScene.prototype.constructor = EditorScene;

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
    /** @type {Phaser.GameObjects.GameObjectFactory} */
    var add = this.add;
    EditorCreate.createWorld(add);

    this.initCamera();
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

EditorScene.prototype.update = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    var x = scene.input.activePointer.worldX;
    var y = scene.input.activePointer.worldY;
    var cam = scene.cameras.main;
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

    cam.scrollX += dx * 30;
    cam.scrollY += dy * 30;
}

EditorScene.prototype.onMouseWheel = function (e) {

    /** @type {Phaser.Scene} */
    var scene = this;
    
    var cam = scene.cameras.main;
    var delta = e.wheelDelta;
    var zoom = (delta < 0 ? 0.9 : 1.1);
    cam.zoom *= zoom;
};