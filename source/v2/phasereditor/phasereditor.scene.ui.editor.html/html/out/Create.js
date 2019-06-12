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
                case "DynamicBitmapText":
                    var x = PhaserEditor2D.TransformComponent.get_x(data);
                    var y = PhaserEditor2D.TransformComponent.get_y(data);
                    var key = PhaserEditor2D.BitmapTextComponent.get_fontAssetKey(data);
                    obj = add.dynamicBitmapText(x, y, key);
                    break;
                case "Text":
                    var x = PhaserEditor2D.TransformComponent.get_x(data);
                    var y = PhaserEditor2D.TransformComponent.get_y(data);
                    var text = PhaserEditor2D.TextualComponent.get_text(data);
                    obj = add.text(x, y, text);
                    break;
            }
            switch (type) {
                case "TileSprite":
                    obj.setInteractive(CreatePixelPerfectCanvasTextureHandler(1));
                    break;
                case "BitmapText":
                case "DynamicBitmapText":
                    obj.setInteractive(BitmapTextHitHandler);
                    break;
                case "Text":
                    obj.setInteractive();
                    break;
                default:
                    obj.setInteractive(PixelPerfectHandler);
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
                    PhaserEditor2D.TextureComponent.updateObject(obj, data);
                    break;
            }
            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                case "BitmapText":
                case "DynamicBitmapText":
                case "Text":
                    PhaserEditor2D.GameObjectEditorComponent.updateObject(obj, data);
                    PhaserEditor2D.TransformComponent.updateObject(obj, data);
                    PhaserEditor2D.OriginComponent.updateObject(obj, data);
                    PhaserEditor2D.FlipComponent.updateObject(obj, data);
                    PhaserEditor2D.TintComponent.updateObject(obj, data);
                    break;
            }
            switch (type) {
                case "TileSprite":
                    PhaserEditor2D.TileSpriteComponent.updateObject(obj, data);
                    break;
                case "BitmapText":
                    PhaserEditor2D.BitmapTextComponent.updateObject(obj, data);
                    break;
                case "DynamicBitmapText":
                    PhaserEditor2D.BitmapTextComponent.updateObject(obj, data);
                    PhaserEditor2D.DynamicBitmapTextComponent.updateObject(obj, data);
                    break;
                case "Text":
                    PhaserEditor2D.TextualComponent.updateObject(obj, data);
                    PhaserEditor2D.TextComponent.updateObject(obj, data);
                    break;
            }
        };
        return Create;
    }());
    PhaserEditor2D.Create = Create;
    function BitmapTextHitHandler(hitArea, x, y, gameObject) {
        var w = gameObject.width / gameObject.scaleX;
        var h = gameObject.height / gameObject.scaleY;
        return x >= 0 && y >= 0 && x <= w && y <= h;
    }
    function CreatePixelPerfectCanvasTextureHandler(alphaTolerance) {
        return function (hitArea, x, y, sprite) {
            if (sprite.flipX) {
                x = 2 * sprite.displayOriginX - x;
            }
            if (sprite.flipY) {
                y = 2 * sprite.displayOriginY - y;
            }
            var alpha = getCanvasTexturePixelAlpha(x, y, sprite.texture);
            return alpha >= alphaTolerance;
        };
    }
    function PixelPerfectHandler(hitArea, x, y, sprite) {
        if (sprite.flipX) {
            x = 2 * sprite.displayOriginX - x;
        }
        if (sprite.flipY) {
            y = 2 * sprite.displayOriginY - y;
        }
        var textureManager = PhaserEditor2D.Editor.getInstance().getGame().textures;
        var alpha = textureManager.getPixelAlpha(x, y, sprite.texture.key, sprite.frame.name);
        return alpha;
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
