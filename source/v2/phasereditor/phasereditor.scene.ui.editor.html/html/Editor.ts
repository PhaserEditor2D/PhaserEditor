namespace PhaserEditor2D {


    export class Editor {

        hitTestPointer(scene: Phaser.Scene, pointer: Phaser.Input.Pointer): any {
            const input: any = scene.game.input;

            const real = input.real_hitTest;
            const fake = input.hitTest;

            input.hitTest = real;

            const result = scene.input.hitTestPointer(pointer);

            input.hitTest = fake;

            return result;
        }


        private static _instance: Editor;
        private _socket: WebSocket;
        private _game: Phaser.Game;        
        private _resizeToken: integer;
        private _objectScene: ObjectScene;
        private _create: Create;
        private _transformLocalCoords: boolean;
        private _pendingMouseDownEvent: MouseEvent;
        private _closed = false;
        private _isReloading = false;

        sceneProperties: any;
        selection: any[] = [];
        private _webgl: boolean;
        private _chromiumWebview : boolean;

        constructor() {
            Editor._instance = this;

            this.openSocket();
        }

        static getInstance() {
            return Editor._instance;
        }

        repaint() {
            consoleLog("repaint");
            this._game.loop.tick();
        }

        stop() {
            consoleLog("loop.stop");
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

        sceneCreated() {
            const self = this;


            this._game.canvas.addEventListener("mousedown", function (e: MouseEvent) {
                if (self._closed) {
                    return;
                }

                if (self.getToolScene().containsPointer()) {
                    self.getToolScene().onMouseDown();
                } else {
                    self.getObjectScene().getDragCameraManager().onMouseDown(e);
                    self.getObjectScene().getPickManager().onMouseDown(e);
                    self.getObjectScene().getDragObjectsManager().onMouseDown(e);
                    //self._pendingMouseDownEvent = e;
                }
            })

            this._game.canvas.addEventListener("mousemove", function (e: MouseEvent) {
                if (self._closed) {
                    return;
                }

                if (self.getToolScene().isEditing()) {
                    self.getToolScene().onMouseMove();
                } else {
                    self.getObjectScene().getDragObjectsManager().onMouseMove(e);
                    self.getObjectScene().getDragCameraManager().onMouseMove(e);
                }

            })

            this._game.canvas.addEventListener("mouseup", function (e : MouseEvent) {
                if (self._closed) {
                    return;
                }

                if (self.getToolScene().isEditing()) {
                    self.getToolScene().onMouseUp();
                } else {
                    //self.getObjectScene().getDragObjectsManager().onMouseUp();
                    self.getObjectScene().getDragCameraManager().onMouseUp();
                    self.getObjectScene().getPickManager().onMouseUp(e);

                    setTimeout(function () {
                        self.getObjectScene().getDragObjectsManager().onMouseUp();
                    }, 30)
                }
            })

            this._game.canvas.addEventListener("mouseleave", function () {
                if (self._closed) {
                    return;
                }

                self.getObjectScene().getDragObjectsManager().onMouseUp();
                self.getObjectScene().getDragCameraManager().onMouseUp();
            })


            this.sendMessage({
                method: "GetInitialState"
            });

        }

        sendKeyDown(e: KeyboardEvent) {
            const data = {
                keyCode: e.keyCode,
                ctrlKey: e.ctrlKey || e.metaKey,
                shiftKey: e.shiftKey,
            };

            this.sendMessage({
                method: "KeyDown",
                data: data
            });
        }

        private onResize() {

            for (let scene of this._game.scene.scenes) {
                const scene2 = <Phaser.Scene>scene;
                scene2.cameras.main.setSize(window.innerWidth, window.innerHeight);
                scene2.scale.resize(window.innerWidth, window.innerHeight);
            }

            this.repaint();
        }

        openSocket() {
            consoleLog("Open socket");
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
                    consoleLog("Closing socket...");
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
            consoleLog("Socket closed");
            if (this._isReloading) {
                consoleLog("Closed because a reload.");
                return;
            }
            this._closed = true;
            let body = document.getElementById("body");
            var elem = document.createElement("div");
            elem.innerHTML = "<p><br><br><br>Lost the connection with Phaser Editor</p><button onclick='document.location.reload()'>Reload</button>";
            elem.setAttribute("class", "lostConnection");
            body.appendChild(elem);
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

            if (this._pendingMouseDownEvent) {
                const e = this._pendingMouseDownEvent;
                this._pendingMouseDownEvent = null;
                this.getObjectScene().getDragObjectsManager().onMouseDown(e);
            }
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
            this._isReloading = true;
            this._socket.close();
            window.location.reload();
        }

        private onUpdateSceneProperties(msg: any) {
            this.sceneProperties = msg.sceneProperties;

            this.getObjectScene().updateBackground();
            this.getToolScene().updateFromSceneProperties();

            this.updateBodyColor();
        }

        private updateBodyColor() {
            const body = document.getElementsByTagName("body")[0];
            body.style.backgroundColor = "rgb(" + ScenePropertiesComponent.get_backgroundColor(this.sceneProperties) + ")";
        }

        private onCreateGame(msg: any) {

            const self = this;

            // update the model

            this._webgl = msg.webgl;
            this._chromiumWebview = msg.chromiumWebview;
            this.sceneProperties = msg.sceneProperties;

            // create the game

            this._create = new Create();
            this._game = new Phaser.Game({
                title: "Phaser Editor 2D - Web Scene Editor",
                width: window.innerWidth,
                height: window.innerWidth,
                // WEBGL is problematic on Linux
                type: this._webgl ? Phaser.WEBGL : Phaser.CANVAS,
                render: {
                    pixelArt: true
                },
                audio: {
                    noAudio: true
                },
                url: "https://phasereditor2d.com",
                scale: {
                    mode: Phaser.Scale.RESIZE
                }
            });

            (<any>this._game.config).postBoot = function (game: Phaser.Game) {
                consoleLog("Game booted");
                setTimeout(() => self.stop(), 500);
            };

            // default hitTest is a NOOP, so it does not run heavy tests in all mouse moves.
            const input: any = this._game.input;
            input.real_hitTest = input.hitTest;
            input.hitTest = function () {
                return [];
            };
            // --

            this._objectScene = new ObjectScene();

            this._game.scene.add("ObjectScene", this._objectScene);
            this._game.scene.add("ToolScene", ToolScene);
            this._game.scene.start("ObjectScene", {
                displayList: msg.displayList,
                projectUrl: msg.projectUrl,
                pack: msg.pack
            });

            this._resizeToken = 0;

            window.addEventListener('resize', function (event) {
                if (self._closed) {
                    return;
                }

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
                if (self._closed) {
                    return;
                }

                self.getObjectScene().onMouseWheel(e);
                self.repaint();
            });

            this.updateBodyColor();
        }

        snapValueX(x: number) {
            const props = this.sceneProperties;
            if (ScenePropertiesComponent.get_snapEnabled(props)) {
                const snap = ScenePropertiesComponent.get_snapWidth(props);
                return Math.round(x / snap) * snap;
            }
            return x;
        }

        snapValueY(y: number) {
            const props = this.sceneProperties;
            if (ScenePropertiesComponent.get_snapEnabled(props)) {
                const snap = ScenePropertiesComponent.get_snapHeight(props);
                return Math.round(y / snap) * snap;
            }
            return y;
        }

        private onDropObjects(msg: any) {
            consoleLog("onDropObjects()");

            const list = msg.list;

            for (let model of list) {
                this._create.createObject(this.getObjectScene(), model);
            }

            this.repaint();
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
            consoleLog("onServerMessage:");
            consoleLog(batch);
            consoleLog("----");

            var list = batch.list;

            this.processMessageList(0, list);

        };

        private onLoadAssets(index: number, list: any[]) {
            let loadMsg = list[index];
            const self = this;

            if (loadMsg.pack) {
                let scene = this.getObjectScene();

                Editor.getInstance().stop();
                scene.load.once(Phaser.Loader.Events.COMPLETE,

                    (function (index2, list2) {
                        return function () {
                            consoleLog("Loader complete.");

                            self.processMessageList(index2, list2);
                        };
                    })(index + 1, list)
                    , this);
                consoleLog("Load: ");
                consoleLog(loadMsg.pack);
                scene.load.crossOrigin = "anonymous";
                scene.load.addPack(loadMsg.pack);
                scene.load.start();
                setTimeout(() => this.repaint(), 100);
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

        isWebGL() {
            return this._webgl;
        }

        isChromiumWebview() {
            return this._chromiumWebview;
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
                x: x,
                y: y
            });
        }

        private onRevealObject(msg) {
            const sprite: Phaser.GameObjects.Sprite = <any>this.getObjectScene().sys.displayList.getByName(msg.id);
            if (sprite) {
                const tx = sprite.getWorldTransformMatrix();
                let p = new Phaser.Math.Vector2();
                tx.transformPoint(0, 0, p);
                const cam = this.getObjectScene().cameras.main;
                cam.setScroll(p.x - cam.width / 2, p.y - cam.height / 2);
            }
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
                    case "RevealObject":
                        this.onRevealObject(msg);
                        break;
                }
            }

            this.repaint();
        }

        sendMessage(msg: any) {
            consoleLog("Sending message:");
            consoleLog(msg);
            consoleLog("----");
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