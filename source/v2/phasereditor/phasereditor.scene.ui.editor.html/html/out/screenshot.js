var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Screenshot;
    (function (Screenshot) {
        var ws;
        var game;
        var MODEL_LIST = [];
        var CURRENT_MODEL;
        function mainScreenshot() {
            connect();
        }
        Screenshot.mainScreenshot = mainScreenshot;
        function connect() {
            ws = new WebSocket(getWebSocketUrl());
            ws.addEventListener("message", onMessage);
            ws.addEventListener("close", onClose);
        }
        function onClose() {
            connect();
        }
        function createGame() {
            game = new Phaser.Game({
                width: CURRENT_MODEL,
                height: 380,
                render: {
                    pixelArt: true
                },
                backgroundColor: "#f0f0f0",
                audio: {
                    noAudio: true
                },
                scale: {
                    mode: Phaser.Scale.NONE
                }
            });
            game.scene.add("Preload", Preload);
            game.scene.add("Level", Level);
        }
        function getWebSocketUrl() {
            var loc = document.location;
            return "ws://" + loc.host + "/ws/api?channel=sceneScreenshot";
        }
        function onMessage(event) {
            var data = JSON.parse(event.data);
            consoleLog("message: " + data.method);
            consoleLog(data);
            if (data.method === "CreateScreenshot") {
                MODEL_LIST.push(data);
                if (MODEL_LIST.length === 1) {
                    nextModel();
                }
            }
        }
        function nextModel() {
            if (game) {
                game.destroy(false);
            }
            if (MODEL_LIST.length > 0) {
                CURRENT_MODEL = MODEL_LIST.pop();
                consoleLog("Start processing new model at project " + CURRENT_MODEL.projectUrl);
                createGame();
                game.scene.start("Preload");
            }
        }
        var Preload = (function (_super) {
            __extends(Preload, _super);
            function Preload() {
                return _super.call(this, "Preload") || this;
            }
            Preload.prototype.preload = function () {
                this.load.setBaseURL(CURRENT_MODEL.projectUrl);
                this.load.pack("pack", CURRENT_MODEL.pack);
            };
            Preload.prototype.create = function () {
                this.scene.start("Level");
            };
            return Preload;
        }(Phaser.Scene));
        var Level = (function (_super) {
            __extends(Level, _super);
            function Level() {
                return _super.call(this, "Level") || this;
            }
            Level.prototype.create = function () {
                var sceneInfo = CURRENT_MODEL.scenes.pop();
                var x = PhaserEditor2D.ScenePropertiesComponent.get_borderX(sceneInfo.model);
                var y = PhaserEditor2D.ScenePropertiesComponent.get_borderY(sceneInfo.model);
                var width = PhaserEditor2D.ScenePropertiesComponent.get_borderWidth(sceneInfo.model);
                var height = PhaserEditor2D.ScenePropertiesComponent.get_borderHeight(sceneInfo.model);
                this.cameras.main.setSize(width, height);
                this.cameras.main.setScroll(x, y);
                this.scale.resize(width, height);
                var create = new PhaserEditor2D.Create(false);
                create.createWorld(this, sceneInfo.model.displayList);
                this.game.renderer.snapshot(function (image) {
                    var imageData = image.src;
                    var _GetDataURL = window.GetDataURL;
                    if (_GetDataURL) {
                        _GetDataURL(imageData);
                    }
                    else {
                        var file = sceneInfo.file;
                        consoleLog("Sending screenshot data of " + file);
                        var loc = document.location;
                        var url = "http://" + loc.host + "/sceneScreenshotService/sceneInfo?" + file;
                        var req = new XMLHttpRequest();
                        req.open("POST", url);
                        req.setRequestHeader("Content-Type", "application/upload");
                        req.send(JSON.stringify({
                            file: file,
                            imageData: imageData
                        }));
                    }
                    if (CURRENT_MODEL.scenes.length === 0) {
                        nextModel();
                    }
                    else {
                        game.scene.start("Level");
                    }
                });
            };
            return Level;
        }(Phaser.Scene));
    })(Screenshot = PhaserEditor2D.Screenshot || (PhaserEditor2D.Screenshot = {}));
})(PhaserEditor2D || (PhaserEditor2D = {}));
window.addEventListener("load", PhaserEditor2D.Screenshot.mainScreenshot);
function consoleLog(msg) {
}
