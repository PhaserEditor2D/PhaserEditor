namespace phasereditor2d.ui.ide.editors.scene {

    export class SceneObjectMaker {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
        }

        createWithDropEvent(e: DragEvent, dropDataArray: any[]): void {
            const scene = this._editor.getGameScene();

            const worldPoint = scene.getCamera().getWorldPoint(e.offsetX, e.offsetY);
            const x = worldPoint.x;
            const y = worldPoint.y;

            for (const data of dropDataArray) {
                this.updateTextureCacheWithAssetData(data);
            }

            for (const data of dropDataArray) {
                if (data instanceof pack.AssetPackImageFrame) {

                    const sprite = scene.add.image(x, y, data.getPackItem().getKey(), data.getName());
                    this.initSprite(sprite);

                } else if (data instanceof pack.AssetPackItem) {
                    switch (data.getType()) {
                        case pack.IMAGE_TYPE: {

                            const sprite = scene.add.image(x, y, data.getKey());
                            this.initSprite(sprite);

                            break;
                        }
                    }
                }
            }

            this._editor.repaint();
        }

        private updateTextureCacheWithAssetData(data: any) {
            const game = this._editor.getGame();

            let imageFrameContainerPackItem: pack.AssetPackItem = null;

            if (data instanceof pack.AssetPackItem && data.getType() === pack.IMAGE_TYPE) {
                imageFrameContainerPackItem = data;
            } else if (data instanceof pack.AssetPackImageFrame) {
                imageFrameContainerPackItem = data.getPackItem();
            }

            if (imageFrameContainerPackItem !== null) {
                const parser = pack.AssetPackUtils.getImageFrameParser(imageFrameContainerPackItem);
                parser.addToPhaserCache(game);
            }

        }

        private initSprite(sprite: Phaser.GameObjects.Sprite | Phaser.GameObjects.Image) {

        }

    }

}