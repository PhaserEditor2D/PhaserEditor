namespace PhaserEditor2D {

    export class Editor {

        private static _instance: Editor;
        private _socket: WebSocket;
        private _game: Phaser.Game;
        private _resizeToken: integer;
        private _objectScene: ObjectScene;
        private _create: Create;
        private _transformLocalCoords: boolean;

        sceneProperties: any;
        selection: any[] = [];

        constructor() {
            Editor._instance = this;

            this.openSocket();
        }

        static getInstance() {
            return Editor._instance;
        }

        repaint() {
            this._game.loop.step(Date.now());
        }

        stop() {
            this._game.loop.stop();
        }

        getCreate() {
            return this._create;
        }

        getGame() {
            return this._game;
        }

        getObjectScene() {
            return this._objectScene;
        }

        getToolScene() {
            return this.getObjectScene().getToolScene();
        }

        getBackgroundScene() {
            return this.getObjectScene().getBackgroundScene();
        }

        sceneCreated() {
            const self = this;


            this._game.canvas.addEventListener("mousedown", function (e: MouseEvent) {
                if (self.getToolScene().containsPointer()) {
                    self.getToolScene().onMouseDown();
                } else {
                    self.getObjectScene().getDragManager().onMouseDown(e);
                    self.getObjectScene().getPickManager().onMouseDown(e);
                }
            })

            this._game.canvas.addEventListener("mousemove", function (e: MouseEvent) {
                if (self.getToolScene().isEditing()) {
                    self.getToolScene().onMouseMove();
                } else {
                    self.getObjectScene().getDragManager().onMouseMove(e);
                }

            })

            this._game.canvas.addEventListener("mouseup", function () {
                if (self.getToolScene().isEditing()) {
                    self.getToolScene().onMouseUp();
                } else {
                    self.getObjectScene().getDragManager().onMouseUp();
                }
            })

            this._game.canvas.addEventListener("mouseleave", function () {
                self.getObjectScene().getDragManager().onMouseUp();
            })


            this.sendMessage({
                method: "GetInitialState"
            });

        }

        private onResize() {

            for (let scene of this._game.scene.scenes) {
                scene.cameras.main.setSize(window.innerWidth, window.innerHeight);
            }

            this.repaint();
        }

        openSocket() {
            console.log("Open socket");
            this._socket = new WebSocket(this.getWebSocketUrl());

            const self = this;

            // we should create the socket when the editor scene is ready, it means, the first time the preload method is called.
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

            window.addEventListener("beforeunload", (event) => {
                if (self._socket) {
                    console.log("Closing socket...");
                    self.closeSocket();
                }
                //event.preventDefault();
                //event.returnValue = "";
            });
        }

        private closeSocket() {
            this._socket.onclose = function () { };
            this._socket.close();
        }

        private onClosedSocket() {
            console.log("Socket closed");
            this._game.destroy(true, false);
            let body = document.getElementById("body");
            body.innerHTML = "<div class='lostConnection'><p>Lost the connection with Phaser Editor</p><button onclick='document.location.reload()'>Reload</button></div>";
            body.style.backgroundColor = "gray";
        }

        private onSelectObjects(msg: any) {
            this.selection = msg.objectIds;

            this.getToolScene().updateSelectionObjects();

            let list = [];
            let point = new Phaser.Math.Vector2(0, 0);
            let tx = new Phaser.GameObjects.Components.TransformMatrix();

            for (let obj of this.getToolScene().getSelectedObjects()) {

                let objTx: Phaser.GameObjects.Components.Transform = <any>obj;
                objTx.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, point);

                let info: any = {
                    id: obj.name
                };

                if (obj instanceof Phaser.GameObjects.BitmapText) {
                    info.displayWidth = obj.width;
                    info.displayHeight = obj.height;
                } else {
                    info.displayWidth = (<any>obj).displayWidth;
                    info.displayHeight = (<any>obj).displayHeight;
                }

                list.push(info);
            }

            this.sendMessage({
                method: "SetObjectDisplayProperties",
                list: list
            });
        };

        private onUpdateObjects(msg) {

            var list = msg.objects;

            for (var i = 0; i < list.length; i++) {
                var objData = list[i];

                var id = objData["-id"];

                var obj = this._objectScene.sys.displayList.getByName(id);

                this._create.updateObject(obj, objData);
            }
        }

        private onReloadPage() {
            this._socket.close();
            window.location.reload();
        }

        private onUpdateSceneProperties(msg: any) {
            this.sceneProperties = msg.sceneProperties;
            this.getToolScene().updateFromSceneProperties();

            this.updateBodyColor();
        }

        private updateBodyColor() {
            const body = document.getElementsByTagName("body")[0];
            body.style.backgroundColor = "rgb(" + ScenePropertiesComponent.get_backgroundColor(this.sceneProperties) + ")";
        }

        private onCreateGame(msg: any) {
            // update the model

            const webgl = msg.webgl;
            this.sceneProperties = msg.sceneProperties;

            // create the game

            this._create = new Create();
            this._game = new Phaser.Game({
                title: "Phaser Editor 2D - Web Scene Editor",
                width: window.innerWidth,
                height: window.innerWidth,
                // WEBGL is problematic on Linux
                type: webgl ? Phaser.WEBGL : Phaser.CANVAS,
                render: {
                    pixelArt: true
                },
                url: "https://phasereditor2d.com",
                scale: {
                    mode: Phaser.Scale.RESIZE
                }
            });

            this._objectScene = new ObjectScene();

            this._game.scene.add("ObjectScene", this._objectScene);
            this._game.scene.add("BackgroundScene", BackgroundScene);
            this._game.scene.add("ToolScene", ToolScene);
            this._game.scene.start("ObjectScene", {
                displayList: msg.displayList,
                projectUrl: msg.projectUrl,
                pack: msg.pack
            });

            this._resizeToken = 0;

            const self = this;

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
        }

        private onDropObjects(msg: any) {
            const list = msg.list;

            for (let model of list) {
                this._create.createObject(this.getObjectScene(), model);
            }
        }

        private onDeleteObjects(msg) {
            let scene = this.getObjectScene();

            let list = msg.list;

            for (let id of list) {
                var obj = scene.sys.displayList.getByName(id);
                if (obj) {
                    obj.destroy();
                }
            }
        }

        private onResetScene(msg: any) {
            let scene = this.getObjectScene();
            scene.removeAllObjects();
            this._create.createWorld(scene, msg.displayList);
        }

        private onRunPositionAction(msg) {
            let actionName = msg.action;

            let action: PositionAction;

            switch (actionName) {
                case "Align":
                    action = new AlignAction(msg);
                    break;
            }

            if (action) {
                action.run();
            }
        }

        private onServerMessage(batch: any) {
            console.log("onServerMessage:");
            console.log(batch);
            console.log("----");

            var list = batch.list;

            this.processMessageList(0, list);

        };

        private _loaderIntervalID: number;

        private onLoadAssets(index: number, list: any[]) {
            let loadMsg = list[index];
            const self = this;

            if (loadMsg.pack) {
                let scene = this.getObjectScene();
                scene.load.once(Phaser.Loader.Events.COMPLETE,

                    (function (index2, list2) {
                        return function () {
                            console.log("Loader complete.");

                            console.log("Cancel " + self._loaderIntervalID);
                            clearInterval(self._loaderIntervalID);

                            self.processMessageList(index2, list2);

                            self.repaint();
                        };
                    })(index + 1, list)


                    , this);
                console.log("Load: ");
                console.log(loadMsg.pack);
                scene.load.addPack(loadMsg.pack);
                scene.load.start();
                this._loaderIntervalID = setInterval(function () {
                    self.repaint();
                }, 20);
            } else {
                this.processMessageList(index + 1, list);
            }

        }

        private onSetObjectOriginKeepPosition(msg: any) {
            let list = msg.list;
            let value = msg.value;
            let is_x_axis = msg.axis === "x";

            let displayList = this.getObjectScene().sys.displayList;


            let point = new Phaser.Math.Vector2();
            let tx = new Phaser.GameObjects.Components.TransformMatrix();

            let data = [];

            for (let id of list) {
                let obj = <any>displayList.getByName(id);

                let x = -obj.width * obj.originX;
                let y = -obj.height * obj.originY;

                obj.getWorldTransformMatrix(tx);
                tx.transformPoint(x, y, point);

                data.push(
                    {
                        obj: obj,
                        x: point.x,
                        y: point.y
                    }
                );
            }

            for (let item of data) {
                let obj = item.obj;

                if (is_x_axis) {
                    obj.setOrigin(value, obj.originY);
                } else {
                    obj.setOrigin(obj.originX, value);
                }
            }

            this.repaint();

            let list2 = [];

            for (let item of data) {
                let obj = item.obj;

                // restore the position!

                let x = -obj.width * obj.originX;
                let y = -obj.height * obj.originY;

                obj.getWorldTransformMatrix(tx);
                tx.transformPoint(x, y, point);

                obj.x += item.x - point.x;
                obj.y += item.y - point.y;

                // build message data

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
        }

        private onSetCameraState(msg: any) {
            let cam = this.getObjectScene().cameras.main;

            if (msg.cameraState.scrollX !== undefined) {
                cam.scrollX = msg.cameraState.scrollX;
                cam.scrollY = msg.cameraState.scrollY;
                cam.zoom = msg.cameraState.zoom;
            }
        }

        private onSetInteractiveTool(msg: any) {
            const tools = [];

            for (let name of msg.list) {
                const tools2 = ToolFactory.createByName(name);
                for (let tool of tools2) {
                    tools.push(tool);
                }
            }
            this._transformLocalCoords = msg.transformLocalCoords;
            this.getToolScene().setTools(tools);
        }

        isTransformLocalCoords() {
            return this._transformLocalCoords;
        }

        private onSetTransformCoords(msg: any) {
            this._transformLocalCoords = msg.transformLocalCoords;
        }

        private onGetPastePosition(msg: any) {
            let x = 0;
            let y = 0;

            if (msg.placeAtCursorPosition) {
                const pointer = this.getObjectScene().input.activePointer;
                const point = this.getObjectScene().getScenePoint(pointer.x, pointer.y);
                x = point.x;
                y = point.y;
            } else {
                let cam = this.getObjectScene().cameras.main;
                x = cam.midPoint.x;
                y = cam.midPoint.y;
            }
            
            this.sendMessage({
                method: "PasteEvent",
                parent: msg.parent,
                x : x,
                y: y
            });
        }

        private processMessageList(startIndex: number, list: any[]) {

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
                        // break the loop, the remaining messages are processed after the load
                        return;
                    case "SetObjectOriginKeepPosition":
                        this.onSetObjectOriginKeepPosition(msg);
                        break;
                    case "SetCameraState":
                        this.onSetCameraState(msg);
                        break;
                    case "SetInteractiveTool":
                        this.onSetInteractiveTool(msg);
                        break;
                    case "SetTransformCoords":
                        this.onSetTransformCoords(msg);
                        break;
                    case "GetPastePosition":
                        this.onGetPastePosition(msg);
                        break;
                }
            }

            this.repaint();
        }

        sendMessage(msg: any) {
            console.log("Sending message:");
            console.log(msg);
            console.log("----");
            this._socket.send(JSON.stringify(msg));
        }

        private getWebSocketUrl() {
            var loc = document.location;
            var channel = this.getChannelId();
            return "ws://" + loc.host + "/ws/api?channel=" + channel;
        }

        private getChannelId() {
            var s = document.location.search;
            var i = s.indexOf("=");
            var c = s.substring(i + 1);
            return c;
        }
    }
}