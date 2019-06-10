namespace PhaserEditor2D {
    export class Create {
        constructor() {

        }

        createWorld(scene: Phaser.Scene, displayList: any) {
            var list = displayList.children;

            for (var i = 0; i < list.length; i++) {
                var data = list[i];
                this.createObject(scene, data);
            }
        }

        createObject(scene: Phaser.Scene, data: any) {

            var type = data["-type"];
            var obj: Phaser.GameObjects.GameObject;
            let add = scene.add;

            switch (type) {
                case "Image":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.image(x, y, key, frame);

                    break;

                case "Sprite":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.sprite(x, y, key, frame);

                    break;

                case "TileSprite":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var width = TileSpriteComponent.get_width(data);
                    var height = TileSpriteComponent.get_height(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.tileSprite(x, y, width, height, key, frame);

                    break;

                case "BitmapText":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = BitmapTextComponent.get_fontAssetKey(data);

                    obj = add.bitmapText(x, y, key);

                    break;

                case "DynamicBitmapText":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = BitmapTextComponent.get_fontAssetKey(data);

                    obj = add.dynamicBitmapText(x, y, key);

                    break;

                case "Text":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);
                    var text = TextualComponent.get_text(data);

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
        }

        updateObject(obj: any, data: any) {
            var type = data["-type"];
            obj.name = data["-id"];

            VisibleComponent.updateObject(obj, data);

            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                case "BitmapText":
                case "DynamicBitmapText":
                case "Text":
                    GameObjectEditorComponent.updateObject(obj, data);
                    TransformComponent.updateObject(obj, data);
                    OriginComponent.updateObject(obj, data);
                    FlipComponent.updateObject(obj, data);
                    TintComponent.updateObject(obj, data);
                    break;
            }

            switch (type) {
                case "TileSprite":
                    TileSpriteComponent.updateObject(obj, data);
                    break;
                case "BitmapText":
                    BitmapTextComponent.updateObject(obj, data);
                    break;
                case "DynamicBitmapText":
                    BitmapTextComponent.updateObject(obj, data);
                    DynamicBitmapTextComponent.updateObject(obj, data);
                    break;
                case "Text":
                    TextualComponent.updateObject(obj, data);
                    TextComponent.updateObject(obj, data);
                    break;
            }
        }
    }

    function BitmapTextHitHandler(hitArea: any, x: number, y: number, gameObject: Phaser.GameObjects.BitmapText) {
        // the bitmaptext width is considered a displayWidth, it is already multiplied by the scale
        let w = gameObject.width / gameObject.scaleX;
        let h = gameObject.height / gameObject.scaleY;

        return x >= 0 && y >= 0 && x <= w && y <= h;
    }

    function CreatePixelPerfectCanvasTextureHandler(alphaTolerance: number) {

        return function (hitArea: any, x: number, y: number, sprite: any) {
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

    function PixelPerfectHandler(hitArea, x, y, sprite: Phaser.GameObjects.Sprite) {
        if (sprite.flipX) {
            x = 2 * sprite.displayOriginX - x;
        }

        if (sprite.flipY) {
            y = 2 * sprite.displayOriginY - y;
        }

        const textureManager = Editor.getInstance().getGame().textures;
        var alpha = textureManager.getPixelAlpha(x, y, sprite.texture.key, sprite.frame.name);

        return alpha;
    };

    function getCanvasTexturePixelAlpha(x: number, y: number, canvasTexture: Phaser.Textures.CanvasTexture) {
        if (canvasTexture) {
            //if (x >= 0 && x < canvasTexture.width && y >= 0 && y < canvasTexture.height) 
            {
                let imgData = canvasTexture.getContext().getImageData(x, y, 1, 1);
                let rgb = imgData.data;
                let alpha = rgb[3];
                return alpha;
            }
        }
        return 0;
    }
}