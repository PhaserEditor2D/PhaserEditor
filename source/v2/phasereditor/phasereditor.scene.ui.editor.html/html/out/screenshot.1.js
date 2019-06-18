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
var scenePath = window.location.search.substring(1);
function mainScreenshot() {
    var game = new Phaser.Game({
        width: 420,
        height: 380,
        render: {
            pixelArt: true
        },
        backgroundColor: "#f0f0f0",
        audio: {
            noAudio: true
        },
        scale: {
            mode: Phaser.Scale.FIT,
            autoCenter: Phaser.Scale.CENTER_BOTH
        }
    });
    game.scene.add("Boot", Boot);
    game.scene.add("Level", Level);
    game.scene.start("Boot");
}
var Boot = (function (_super) {
    __extends(Boot, _super);
    function Boot() {
        return _super.call(this, "Boot") || this;
    }
    Boot.prototype.preload = function () {
        this.load.json("info", "/sceneScreenshotService/sceneInfo/" + scenePath);
    };
    Boot.prototype.create = function () {
        this.scene.start("Level");
    };
    return Boot;
}(Phaser.Scene));
var Level = (function (_super) {
    __extends(Level, _super);
    function Level() {
        return _super.call(this, "Level") || this;
    }
    Level.prototype.preload = function () {
        var info = this.cache.json.get("info");
        this.load.setBaseURL(info.projectUrl);
        this.load.pack("pack", info.pack);
    };
    Level.prototype.create = function () {
        var info = this.cache.json.get("info");
        var create = new PhaserEditor2D.Create(false);
        create.createWorld(this, info.sceneModel.displayList);
        this.game.renderer.snapshot(function (image) {
            var data = image.src;
            var _GetDataURL = window.GetDataURL;
            if (_GetDataURL) {
                _GetDataURL(data);
            }
            else {
                var loc = document.location;
                var url = "http://" + loc.host + "/sceneScreenshotService/sceneInfo?" + scenePath;
                var req = new XMLHttpRequest();
                req.open("POST", url);
                req.setRequestHeader("Content-Type", "application/upload");
                req.send(data);
            }
        });
    };
    return Level;
}(Phaser.Scene));
window.addEventListener("load", mainScreenshot);
