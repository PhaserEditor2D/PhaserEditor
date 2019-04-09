namespace PhaserEditor2D {

    export class Editor {

        private static _instance: Editor;
        private _socket: WebSocket;
        private _game: Phaser.Game;
        private _resizeToken: integer;
        private _objectScene: ObjectScene;
        private _create: Create;

        sceneProperties : any;
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
                console.log("Socket closed");
            };

            window.addEventListener("beforeunload", (event) => {
                if (self._socket) {
                    console.log("Closing socket...");
                    self._socket.close();
                }
                //event.preventDefault();
                //event.returnValue = "";
            });
        }

        private onSelectObjects(msg: any) {
            this.selection = msg.objectIds;
            this.getToolScene().updateSelectionObjects();
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
                //parent: "editorContainer",
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

            if (msg.pack) {
                let scene = this.getObjectScene();
                scene.load.on(Phaser.Loader.Events.COMPLETE,

                    (function (models) {
                        return function () {

                            console.log("load complete!");

                            for (let model of models) {
                                this._create.createObject(this._objectScene.add, model);
                            }

                            this.repaint();
                        };
                    })(list)


                    , this);
                console.log("Load: ");
                console.log(msg.pack);
                scene.load.addPack(msg.pack);
                scene.load.start();
            } else {
                for (let model of list) {
                    this._create.createObject(this._objectScene.add, model);
                }
            }
        }

        private onServerMessage(batch: any) {
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
                }
            }

            this.repaint();
        };


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