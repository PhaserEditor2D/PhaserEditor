namespace phasereditor2d.ui.ide.editors.scene {

    let ws: WebSocket;
    let game: Phaser.Game;

    let MODEL_LIST = [];
    let CURRENT_MODEL: any;

    export function mainScreenshot() {
       connect();
    }

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

    function onMessage(event: MessageEvent) {
        const data = JSON.parse(event.data);

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
        //window.location.reload();
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

    class Preload extends Phaser.Scene {
        constructor() {
            super("Preload");
        }

        preload() {
            this.load.setBaseURL(CURRENT_MODEL.projectUrl);
            this.load.pack("pack", CURRENT_MODEL.pack);
        }

        create() {
            this.scene.start("Level");
        }
    }

    class Level extends Phaser.Scene {
        constructor() {
            super("Level");
        }

        create() {
            const sceneInfo = CURRENT_MODEL.scenes.pop();
            var x = ScenePropertiesComponent.get_borderX(sceneInfo.model);
            var y = ScenePropertiesComponent.get_borderY(sceneInfo.model);
            var width = ScenePropertiesComponent.get_borderWidth(sceneInfo.model);
            var height = ScenePropertiesComponent.get_borderHeight(sceneInfo.model);

            this.cameras.main.setSize(width, height);
            this.cameras.main.setScroll(x, y);
            this.scale.resize(width, height);

            var create = new Create(false);
            create.createWorld(this, sceneInfo.model.displayList);

            this.game.renderer.snapshot(function (image: HTMLImageElement) {
                var imageData = image.src;
                const _GetDataURL = (<any>window).GetDataURL;
                if (_GetDataURL) {
                    _GetDataURL(imageData);
                } else {
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
                } else {
                    game.scene.start("Level");
                }
            });
        }

    }
}

window.addEventListener("load", phasereditor2d.ui.ide.editors.scene.mainScreenshot);