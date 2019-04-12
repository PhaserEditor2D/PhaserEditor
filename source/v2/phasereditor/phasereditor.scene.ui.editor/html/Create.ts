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
                    GameObjectEditorComponent.updateObject(obj, data);
                    TransformComponent.updateObject(obj, data);
                    OriginComponent.updateObject(obj, data);
                    FlipComponent.updateObject(obj, data);
                    break;
            }

            switch (type) {
                case "TileSprite":
                    TileSpriteComponent.updateObject(obj, data);
                    break;
                case "BitmapText":
                    BitmapTextComponent.updateObject(obj, data);
                    break;
            }
        }
    }

    function CreatePixelPerfectCanvasTextureHandler(alphaTolerance: number) {

        return function (hitArea: any, x: number, y: number, gameObject: any) {

            var alpha = getCanvasTexturePixelAlpha(x, y, gameObject.texture);

            return alpha >= alphaTolerance;
        };

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