var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Create = (function () {
        function Create() {
        }
        Create.prototype.createWorld = function (add) {
            if (!PhaserEditor2D.Models.isReady()) {
                return;
            }
            var list = PhaserEditor2D.Models.displayList.children;
            for (var i = 0; i < list.length; i++) {
                var data = list[i];
                this.createObject(add, data);
            }
        };
        Create.prototype.createObject = function (add, data) {
            var type = data["-type"];
            var obj;
            switch (type) {
                case "Image":
                    var x = PhaserEditor2D.TransformComponent.get_x(data);
                    var y = PhaserEditor2D.TransformComponent.get_y(data);
                    var key = PhaserEditor2D.TextureComponent.get_textureKey(data);
                    var frame = PhaserEditor2D.TextureComponent.get_textureFrame(data);
                    obj = add.image(x, y, key, frame);
                    break;
                case "Sprite":
                    var x = PhaserEditor2D.TransformComponent.get_x(data);
                    var y = PhaserEditor2D.TransformComponent.get_y(data);
                    var key = PhaserEditor2D.TextureComponent.get_textureKey(data);
                    var frame = PhaserEditor2D.TextureComponent.get_textureFrame(data);
                    obj = add.sprite(x, y, key, frame);
                    break;
                case "TileSprite":
                    var x = PhaserEditor2D.TransformComponent.get_x(data);
                    var y = PhaserEditor2D.TransformComponent.get_y(data);
                    var width = PhaserEditor2D.TileSpriteComponent.get_width(data);
                    var height = PhaserEditor2D.TileSpriteComponent.get_height(data);
                    var key = PhaserEditor2D.TextureComponent.get_textureKey(data);
                    var frame = PhaserEditor2D.TextureComponent.get_textureFrame(data);
                    obj = add.tileSprite(x, y, width, height, key, frame);
                    break;
            }
            this.updateObject(obj, data);
        };
        Create.prototype.updateObject = function (obj, data) {
            var type = data["-type"];
            obj.name = data["-id"];
            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                    PhaserEditor2D.TransformComponent.updateObject(obj, data);
                    PhaserEditor2D.OriginComponent.updateObject(obj, data);
                    PhaserEditor2D.FlipComponent.updateObject(obj, data);
                    break;
            }
            if (type === "TileSprite") {
                PhaserEditor2D.TileSpriteComponent.updateObject(obj, data);
            }
        };
        return Create;
    }());
    PhaserEditor2D.Create = Create;
})(PhaserEditor2D || (PhaserEditor2D = {}));
