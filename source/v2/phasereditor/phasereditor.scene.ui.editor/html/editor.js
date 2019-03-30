var EditorGlobal = {
    editor: null,
    toolScene: null
};

/**
 * 
 * @param {Broker} broker 
 */
function Editor() {

    EditorGlobal.editor = this;

    var self = this;

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
        scene: [EditorScene, EditorToolScene],
        backgroundColor: "#d3d3d3"
    });

    this._resizeToken = 0;

    window.addEventListener('resize', function (event) {
        self._resizeToken += 1;
        setTimeout((function (token) {
            return function () {
                if (token === self._resizeToken) {
                    self.getScene().performResize();
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

Editor.prototype.openSocket = function () {
    var self = this;
    
    console.log("Opening socket");

    this._socket = new WebSocket(this.getWebSocketUrl());

    // we should create the socket when the editor scene is ready, it means, the first time the preload method is called.
    this._socket.onopen = function () {
        self.sendMessage({
            method: "GetRefreshAll"
        });
    };

    this._socket.onmessage = function (event) {
        var msg = JSON.parse(event.data);
        self.messageReceived(msg);
    };
};

Editor.prototype.getChannelId = function () {
    var s = document.location.search;
    var i = s.indexOf("=");
    var c = s.substring(i + 1);
    return c;
}

Editor.prototype.getWebSocketUrl = function () {
    var loc = document.location;
    var channel = this.getChannelId();
    return "ws://" + loc.host + "/ws/api?channel=" + channel;
}

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
    var list = batch.list;

    for (var i = 0; i < list.length; i++) {
        var msg = list[i];

        var method = msg.method;

        switch (method) {
            case "RefreshAll":
                this.onRefreshAll(msg);
                break;
            case "UpdateObjects":
                this.onUpdateObjects(msg);
                break;
            case "SelectObjects":
                this.onSelectObjects(msg);
                break;
        }
    }
};

Editor.prototype.onSelectObjects = function (msg) {
    Models.selection = msg.objectIds;
    EditorGlobal.toolScene.updateSelectionObjects();
};

Editor.prototype.onUpdateObjects = function (msg) {
    /** @type {Phaser.Scene} */
    var scene = this.getScene();

    var list = msg.objects;

    for (var i = 0; i < list.length; i++) {
        var objData = list[i];

        Models.displayList_updateObjectData(objData);

        var id = objData["-id"];
        var obj = scene.sys.displayList.getByName(id);
        EditorCreate.updateObject(obj, objData);
    }
}

Editor.prototype.onRefreshAll = function (msg) {
    /** @type {Phaser.Scene} */
    var editorScene = this.getScene();

    Models.displayList = msg.displayList;
    Models.projectUrl = msg.projectUrl;
    Models.packs = msg.packs;

    editorScene.scene.restart();
}