namespace PhaserEditor2D {

    export class Editor {
        private static _instance: Editor;
        private _socket: WebSocket;
        private _game: Phaser.Game;
        private _resizeToken: integer;
        private _objectScene: ObjectScene;
        private _create : Create;

        constructor() {
            Editor._instance = this;

            this._create = new Create();

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
                backgroundColor: "#d3d3d3"
            });

            this._objectScene = new ObjectScene();

            this._game.scene.add("ObjectScene", this._objectScene);
            this._game.scene.add("ToolScene", ToolScene);
            this._game.scene.start("ObjectScene");

            this._resizeToken = 0;

            const self = this;

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

            window.addEventListener("mousewheel", function (e) {
                self.getObjectScene().onMouseWheel(e);
            });
        }

        static getInstance() {
            return Editor._instance;
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

        private performResize() {
            var w = window.innerWidth;
            var h = window.innerHeight;
            this._objectScene.scale.resize(w, h);
        }

        openSocket() {
            this._socket = new WebSocket(this.getWebSocketUrl());
            
            const self = this;

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
        }
        
        private onSelectObjects(msg : any) {
            Models.selection = msg.objectIds;
            this.getToolScene().updateSelectionObjects();
        };

        private onUpdateObjects(msg) {
            
            var list = msg.objects;
        
            for (var i = 0; i < list.length; i++) {
                var objData = list[i];
        
                Models.displayList_updateObjectData(objData);
        
                var id = objData["-id"];
                
                var obj = this._objectScene.sys.displayList.getByName(id);

                this._create.updateObject(obj, objData);
            }
        }

        private onRefreshAll(msg) {
            Models.displayList = msg.displayList;
            Models.projectUrl = msg.projectUrl;
            Models.packs = msg.packs;
        
            this._objectScene.scene.restart();
        }

        private messageReceived(batch : any) {
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

        sendMessage(msg : any) {
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