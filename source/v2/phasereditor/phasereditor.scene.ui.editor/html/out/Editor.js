var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Editor = (function () {
        function Editor() {
            this.selection = [];
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
        Editor.prototype.getBackgroundScene = function () {
            return this.getObjectScene().getBackgroundScene();
        };
        Editor.prototype.onResize = function () {
            for (var _i = 0, _a = this._game.scene.scenes; _i < _a.length; _i++) {
                var scene = _a[_i];
                scene.cameras.main.setSize(window.innerWidth, window.innerHeight);
            }
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
            this.selection = msg.objectIds;
            this.getToolScene().updateSelectionObjects();
        };
        ;
        Editor.prototype.onUpdateObjects = function (msg) {
            var list = msg.objects;
            for (var i = 0; i < list.length; i++) {
                var objData = list[i];
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
            this.sceneProperties = msg.sceneProperties;
            this.getToolScene().updateFromSceneProperties();
            this.updateBodyColor();
        };
        Editor.prototype.updateBodyColor = function () {
            var body = document.getElementsByTagName("body")[0];
            body.style.backgroundColor = "rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_backgroundColor(this.sceneProperties) + ")";
        };
        Editor.prototype.onCreateGame = function (msg) {
            var webgl = msg.webgl;
            this.sceneProperties = msg.sceneProperties;
            this._create = new PhaserEditor2D.Create();
            this._game = new Phaser.Game({
                title: "Phaser Editor 2D - Web Scene Editor",
                width: window.innerWidth,
                height: window.innerWidth,
                type: webgl ? Phaser.WEBGL : Phaser.CANVAS,
                render: {
                    pixelArt: true
                },
                url: "https://phasereditor2d.com",
                scale: {
                    mode: Phaser.Scale.RESIZE
                }
            });
            this._objectScene = new PhaserEditor2D.ObjectScene();
            this._game.scene.add("ObjectScene", this._objectScene);
            this._game.scene.add("BackgroundScene", PhaserEditor2D.BackgroundScene);
            this._game.scene.add("ToolScene", PhaserEditor2D.ToolScene);
            this._game.scene.start("ObjectScene", {
                displayList: msg.displayList,
                projectUrl: msg.projectUrl,
                pack: msg.pack
            });
            this._resizeToken = 0;
            var self = this;
            window.addEventListener('resize', function (event) {
                self._resizeToken += 1;
                setTimeout((function (token) {
                    return function () {
                        if (token === self._resizeToken) {
                            self.onResize();
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
        Editor.prototype.onDropObjects = function (msg) {
            var list = msg.list;
            if (msg.pack) {
                var scene = this.getObjectScene();
                scene.load.once(Phaser.Loader.Events.COMPLETE, (function (models) {
                    return function () {
                        console.log("load complete!");
                        for (var _i = 0, models_1 = models; _i < models_1.length; _i++) {
                            var model = models_1[_i];
                            this._create.createObject(this._objectScene.add, model);
                        }
                        this.repaint();
                    };
                })(list), this);
                console.log("Load: ");
                console.log(msg.pack);
                scene.load.addPack(msg.pack);
                scene.load.start();
            }
            else {
                for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                    var model = list_1[_i];
                    this._create.createObject(this._objectScene.add, model);
                }
            }
        };
        Editor.prototype.onDeleteObjects = function (msg) {
            var scene = this.getObjectScene();
            var list = msg.list;
            for (var _i = 0, list_2 = list; _i < list_2.length; _i++) {
                var id = list_2[_i];
                var obj = scene.sys.displayList.getByName(id);
                if (obj) {
                    obj.destroy();
                }
            }
        };
        Editor.prototype.onResetScene = function (msg) {
            var scene = this.getObjectScene();
            scene.removeAllObjects();
            this._create.createWorld(scene.add, msg.displayList);
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
                    case "DropObjects":
                        this.onDropObjects(msg);
                        break;
                    case "DeleteObjects":
                        this.onDeleteObjects(msg);
                        break;
                    case "ResetScene":
                        this.onResetScene(msg);
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
