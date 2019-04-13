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
                self.onClosedSocket();
            };
            window.addEventListener("beforeunload", function (event) {
                if (self._socket) {
                    console.log("Closing socket...");
                    self._socket.close();
                }
            });
        };
        Editor.prototype.onClosedSocket = function () {
            console.log("Socket closed");
            this._game.destroy(true, false);
            var body = document.getElementById("body");
            body.innerHTML = "<div class='lostConnection'><p>Lost the connection with Phaser Editor</p><button onclick='document.location.reload()'>Reload</button></div>";
            body.style.backgroundColor = "gray";
        };
        Editor.prototype.onSelectObjects = function (msg) {
            this.selection = msg.objectIds;
            this.getToolScene().updateSelectionObjects();
            var list = [];
            var point = new Phaser.Math.Vector2(0, 0);
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            for (var _i = 0, _a = this.getToolScene().getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var objTx = obj;
                objTx.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, point);
                var info = {
                    id: obj.name
                };
                if (obj instanceof Phaser.GameObjects.BitmapText) {
                    info.displayWidth = obj.width;
                    info.displayHeight = obj.height;
                }
                else {
                    info.displayWidth = obj.displayWidth;
                    info.displayHeight = obj.displayHeight;
                }
                list.push(info);
            }
            this.sendMessage({
                method: "SetObjectDisplayProperties",
                list: list
            });
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
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var model = list_1[_i];
                this._create.createObject(this.getObjectScene(), model);
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
            this._create.createWorld(scene, msg.displayList);
        };
        Editor.prototype.onRunPositionAction = function (msg) {
            var actionName = msg.action;
            var action;
            switch (actionName) {
                case "Align":
                    action = new PhaserEditor2D.AlignAction(msg);
                    break;
            }
            if (action) {
                action.run();
            }
        };
        Editor.prototype.onServerMessage = function (batch) {
            console.log("onServerMessage:");
            console.log(batch);
            console.log("----");
            var list = batch.list;
            this.processServerMessages(0, list);
            this.repaint();
        };
        ;
        Editor.prototype.onLoadAssets = function (index, list) {
            var loadMsg = list[index];
            if (loadMsg.pack) {
                var scene = this.getObjectScene();
                scene.load.once(Phaser.Loader.Events.COMPLETE, (function (index2, list2) {
                    return function () {
                        console.log("Loader complete.");
                        this.processServerMessages(index2, list2);
                        this.repaint();
                    };
                })(index + 1, list), this);
                console.log("Load: ");
                console.log(loadMsg.pack);
                scene.load.addPack(loadMsg.pack);
                scene.load.start();
            }
            else {
                this.processServerMessages(index + 1, list);
            }
        };
        Editor.prototype.onSetObjectOriginKeepPosition = function (msg) {
            var list = msg.list;
            var value = msg.value;
            var is_x_axis = msg.axis === "x";
            var displayList = this.getObjectScene().sys.displayList;
            var point = new Phaser.Math.Vector2();
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            var data = [];
            for (var _i = 0, list_3 = list; _i < list_3.length; _i++) {
                var id = list_3[_i];
                var obj = displayList.getByName(id);
                var x = -obj.width * obj.originX;
                var y = -obj.height * obj.originY;
                obj.getWorldTransformMatrix(tx);
                tx.transformPoint(x, y, point);
                data.push({
                    obj: obj,
                    x: point.x,
                    y: point.y
                });
            }
            for (var _a = 0, data_1 = data; _a < data_1.length; _a++) {
                var item = data_1[_a];
                var obj = item.obj;
                if (is_x_axis) {
                    obj.setOrigin(value, obj.originY);
                }
                else {
                    obj.setOrigin(obj.originX, value);
                }
            }
            this.repaint();
            var list2 = [];
            for (var _b = 0, data_2 = data; _b < data_2.length; _b++) {
                var item = data_2[_b];
                var obj = item.obj;
                var x = -obj.width * obj.originX;
                var y = -obj.height * obj.originY;
                obj.getWorldTransformMatrix(tx);
                tx.transformPoint(x, y, point);
                obj.x += item.x - point.x;
                obj.y += item.y - point.y;
                list2.push({
                    id: obj.name,
                    originX: obj.originX,
                    originY: obj.originY,
                    x: obj.x,
                    y: obj.y
                });
            }
            Editor.getInstance().sendMessage({
                method: "SetObjectOrigin",
                list: list2
            });
        };
        Editor.prototype.processServerMessages = function (startIndex, list) {
            for (var i = startIndex; i < list.length; i++) {
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
                    case "RunPositionAction":
                        this.onRunPositionAction(msg);
                        break;
                    case "LoadAssets":
                        this.onLoadAssets(i, list);
                        return;
                    case "SetObjectOriginKeepPosition":
                        this.onSetObjectOriginKeepPosition(msg);
                        break;
                }
            }
        };
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
