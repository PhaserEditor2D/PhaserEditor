var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Create = (function () {
        function Create() {
        }
        Create.prototype.createWorld = function (scene, displayList) {
            var list = displayList.children;
            for (var i = 0; i < list.length; i++) {
                var data = list[i];
                this.createObject(scene, data);
            }
        };
        Create.prototype.createObject = function (scene, data) {
            var type = data["-type"];
            var obj;
            var add = scene.add;
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
                case "BitmapText":
                    var x = PhaserEditor2D.TransformComponent.get_x(data);
                    var y = PhaserEditor2D.TransformComponent.get_y(data);
                    var key = PhaserEditor2D.BitmapTextComponent.get_fontAssetKey(data);
                    obj = add.bitmapText(x, y, key);
                    break;
            }
            switch (type) {
                case "TileSprite":
                    obj.setInteractive(CreatePixelPerfectCanvasTextureHandler(1));
                    break;
                case "BitmapText":
                    obj.setInteractive();
                    break;
                default:
                    obj.setInteractive(scene.input.makePixelPerfect());
                    break;
            }
            this.updateObject(obj, data);
        };
        Create.prototype.updateObject = function (obj, data) {
            var type = data["-type"];
            obj.name = data["-id"];
            PhaserEditor2D.VisibleComponent.updateObject(obj, data);
            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                case "BitmapText":
                    PhaserEditor2D.GameObjectEditorComponent.updateObject(obj, data);
                    PhaserEditor2D.TransformComponent.updateObject(obj, data);
                    PhaserEditor2D.OriginComponent.updateObject(obj, data);
                    PhaserEditor2D.FlipComponent.updateObject(obj, data);
                    break;
            }
            switch (type) {
                case "TileSprite":
                    PhaserEditor2D.TileSpriteComponent.updateObject(obj, data);
                    break;
                case "BitmapText":
                    PhaserEditor2D.BitmapTextComponent.updateObject(obj, data);
                    break;
            }
        };
        return Create;
    }());
    PhaserEditor2D.Create = Create;
    function CreatePixelPerfectCanvasTextureHandler(alphaTolerance) {
        return function (hitArea, x, y, gameObject) {
            var alpha = getCanvasTexturePixelAlpha(x, y, gameObject.texture);
            return alpha >= alphaTolerance;
        };
    }
    ;
    function getCanvasTexturePixelAlpha(x, y, canvasTexture) {
        if (canvasTexture) {
            {
                var imgData = canvasTexture.getContext().getImageData(x, y, 1, 1);
                var rgb = imgData.data;
                var alpha = rgb[3];
                return alpha;
            }
        }
        return 0;
    }
})(PhaserEditor2D || (PhaserEditor2D = {}));
