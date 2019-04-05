var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Editor = (function () {
        function Editor() {
            Editor._instance = this;
            this.openSocket();
        }
        Editor.getInstance = function () {
            return Editor._instance;
        };
        Editor.prototype.repaint = function () {
            this._game.loop.step(Date.now());
        };
        Editor.prototype.stop = function () {
            this._game.loop.stop();
        };
        Editor.prototype.getCreate = function () {
            return this._create;
        };
        Editor.prototype.getGame = function () {
            return this._game;
        };
        Editor.prototype.getObjectScene = function () {
            return this._objectScene;
        };
        Editor.prototype.getToolScene = function () {
            return this.getObjectScene().getToolScene();
        };
        Editor.prototype.performResize = function () {
            this._objectScene.performResize();
            this.repaint();
        };
        Editor.prototype.openSocket = function () {
            console.log("Open socket");
            this._socket = new WebSocket(this.getWebSocketUrl());
            var self = this;
            this._socket.onopen = function () {
                self.sendMessage({
                    method: "GetCreateGame"
                });
            };
            this._socket.onmessage = function (event) {
                var msg = JSON.parse(event.data);
                self.onServerMessage(msg);
            };
            this._socket.onclose = function (event) {
                console.log("Socket closed");
            };
            window.addEventListener("beforeunload", function (event) {
                if (self._socket) {
                    console.log("Closing socket...");
                    self._socket.close();
                }
            });
        };
        Editor.prototype.onSelectObjects = function (msg) {
            PhaserEditor2D.Models.selection = msg.objectIds;
            this.getToolScene().updateSelectionObjects();
        };
        ;
        Editor.prototype.onUpdateObjects = function (msg) {
            var list = msg.objects;
            for (var i = 0; i < list.length; i++) {
                var objData = list[i];
                PhaserEditor2D.Models.displayList_updateObjectData(objData);
                var id = objData["-id"];
                var obj = this._objectScene.sys.displayList.getByName(id);
                this._create.updateObject(obj, objData);
            }
        };
        Editor.prototype.onReloadPage = function () {
            this._socket.close();
            window.location.reload();
        };
        Editor.prototype.onUpdateSceneProperties = function (msg) {
            PhaserEditor2D.Models.sceneProperties = msg.sceneProperties;
            this.getToolScene().updateFromSceneProperties();
            this.updateBodyColor();
        };
        Editor.prototype.updateBodyColor = function () {
            var body = document.getElementsByTagName("body")[0];
            body.setAttribute("style", "background-color: rgb(" + PhaserEditor2D.Models.sceneProperties.backgroundColor + ")");
        };
        Editor.prototype.onCreateGame = function (msg) {
            PhaserEditor2D.Models.gameConfig.webgl = msg.webgl;
            PhaserEditor2D.Models.displayList = msg.displayList;
            PhaserEditor2D.Models.projectUrl = msg.projectUrl;
            PhaserEditor2D.Models.pack = msg.pack;
            PhaserEditor2D.Models.sceneProperties = msg.sceneProperties;
            this._create = new PhaserEditor2D.Create();
            this._game = new Phaser.Game({
                title: "Phaser Editor 2D - Web Scene Editor",
                width: window.innerWidth,
                height: window.innerWidth,
                type: PhaserEditor2D.Models.gameConfig.webgl ? Phaser.WEBGL : Phaser.CANVAS,
                render: {
                    pixelArt: true
                },
                url: "https://phasereditor2d.com",
                parent: "editorContainer",
                scale: {
                    mode: Phaser.Scale.NONE,
                    autoCenter: Phaser.Scale.NO_CENTER
                }
            });
            this._objectScene = new PhaserEditor2D.ObjectScene();
            this._game.scene.add("ObjectScene", this._objectScene);
            this._game.scene.add("BackgroundScene", PhaserEditor2D.BackgroundScene);
            this._game.scene.add("ToolScene", PhaserEditor2D.ToolScene);
            this._game.scene.start("ObjectScene");
            this._resizeToken = 0;
            var self = this;
            window.addEventListener('resize', function (event) {
                self._resizeToken += 1;
                setTimeout((function (token) {
                    return function () {
                        if (token === self._resizeToken) {
                            self.performResize();
                        }
                    };
                })(self._resizeToken), 200);
            }, false);
            window.addEventListener("wheel", function (e) {
                self.getObjectScene().onMouseWheel(e);
                self.repaint();
            });
            this.updateBodyColor();
        };
        Editor.prototype.onServerMessage = function (batch) {
            console.log("onServerMessage:");
            console.log(batch);
            console.log("----");
            var list = batch.list;
            for (var i = 0; i < list.length; i++) {
                var msg = list[i];
                var method = msg.method;
                switch (method) {
                    case "ReloadPage":
                        this.onReloadPage();
                        break;
                    case "CreateGame":
                        this.onCreateGame(msg);
                        break;
                    case "UpdateObjects":
                        this.onUpdateObjects(msg);
                        break;
                    case "SelectObjects":
                        this.onSelectObjects(msg);
                        break;
                    case "UpdateSceneProperties":
                        this.onUpdateSceneProperties(msg);
                        break;
                }
            }
            this.repaint();
        };
        ;
        Editor.prototype.sendMessage = function (msg) {
            console.log("Sending message:");
            console.log(msg);
            console.log("----");
            this._socket.send(JSON.stringify(msg));
        };
        Editor.prototype.getWebSocketUrl = function () {
            var loc = document.location;
            var channel = this.getChannelId();
            return "ws://" + loc.host + "/ws/api?channel=" + channel;
        };
        Editor.prototype.getChannelId = function () {
            var s = document.location.search;
            var i = s.indexOf("=");
            var c = s.substring(i + 1);
            return c;
        };
        return Editor;
    }());
    PhaserEditor2D.Editor = Editor;
})(PhaserEditor2D || (PhaserEditor2D = {}));
