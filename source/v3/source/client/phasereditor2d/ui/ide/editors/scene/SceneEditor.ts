/// <reference path="../../EditorBlocksProvider.ts" />

namespace phasereditor2d.ui.ide.editors.scene {

    import io = core.io;

    class SceneEditorFactory extends EditorFactory {

        constructor() {
            super("phasereditor2d.SceneEditorFactory");
        }

        acceptInput(input: any): boolean {
            if (input instanceof io.FilePath) {
                const contentType = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                return contentType === CONTENT_TYPE_SCENE;
            }
            return false;
        }

        createEditor(): EditorPart {
            return new SceneEditor();
        }


    }

    export class SceneEditor extends FileEditor {

        private _blocksProvider: SceneEditorBlocksProvider;
        private _game: Phaser.Game;
        private _background: SceneEditorBackground;
        private _gameCanvas: HTMLCanvasElement;
        private _gameScene: GameScene;
        private _objectFactory: SceneObjectFactory;
        private _dropManager: DropManager;

        static getFactory(): EditorFactory {
            return new SceneEditorFactory();
        }

        constructor() {
            super("phasereditor2d.SceneEditor");

            this._blocksProvider = new SceneEditorBlocksProvider();
        }

        protected createPart() {

            this.setLayoutChildren(false);

            this._background = new SceneEditorBackground(this);
            this.getElement().appendChild(this._background.getCanvas());

            this._gameCanvas = document.createElement("canvas");
            this._gameCanvas.style.position = "absolute";
            this.getElement().appendChild(this._gameCanvas);

            // init managers and factories

            this._objectFactory = new SceneObjectFactory(this);
            this._dropManager = new DropManager(this);

            // create game scene

            this._gameScene = new GameScene(this);

            this._game = new Phaser.Game({
                type: Phaser.WEBGL,
                canvas: this._gameCanvas,
                scale: {
                    mode: Phaser.Scale.NONE
                },
                render: {
                    pixelArt: true,
                    transparent: true
                },
                audio: {
                    noAudio: true
                },
                scene: this._gameScene
            });

        }

        getGameCanvas() {
            return this._gameCanvas;
        }

        getGameScene() {
            return this._gameScene;
        }

        getObjectFactory() {
            return this._objectFactory;
        }

        layout() {
            super.layout();

            this._background.resizeTo();
            this._background.render();

            const parent = this.getElement().parentElement;
            this._game.scale.resize(parent.clientWidth, parent.clientHeight);
        }

        getBlocksProvider() {
            return this._blocksProvider;
        }
    }

}