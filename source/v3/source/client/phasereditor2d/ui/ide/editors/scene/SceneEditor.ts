/// <reference path="../../EditorViewerProvider.ts" />
/// <reference path="./outline/SceneEditorOutlineProvider.ts" />

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

        private _blocksProvider: blocks.SceneEditorBlocksProvider;
        private _outlineProvider: outline.SceneEditorOutlineProvider;
        private _game: Phaser.Game;
        private _overlayLayer: OverlayLayer;
        private _gameCanvas: HTMLCanvasElement;
        private _gameScene: GameScene;
        private _objectMaker: SceneObjectMaker;
        private _dropManager: DropManager;
        private _cameraManager: CameraManager;
        private _selectionManager: SelectionManager;
        private _gameBooted: boolean;

        static getFactory(): EditorFactory {
            return new SceneEditorFactory();
        }

        constructor() {
            super("phasereditor2d.SceneEditor");

            this._blocksProvider = new blocks.SceneEditorBlocksProvider();
            this._outlineProvider = new outline.SceneEditorOutlineProvider(this);
        }

        protected createPart() {

            this.setLayoutChildren(false);

            this._gameCanvas = document.createElement("canvas");
            this._gameCanvas.style.position = "absolute";
            this.getElement().appendChild(this._gameCanvas);

            this._overlayLayer = new OverlayLayer(this);
            this.getElement().appendChild(this._overlayLayer.getCanvas());


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
                scene: this._gameScene,
            });

            this._gameBooted = false;

            (<any>this._game.config).postBoot = () => {
                this.onGameBoot();
            };

            this._game.loop.stop();

            // init managers and factories

            this._objectMaker = new SceneObjectMaker(this);
            this._dropManager = new DropManager(this);
            this._cameraManager = new CameraManager(this);
            this._selectionManager = new SelectionManager(this);
        }

        getOverlayLayer() {
            return this._overlayLayer;
        }

        getGameCanvas() {
            return this._gameCanvas;
        }

        getGameScene() {
            return this._gameScene;
        }

        getGame() {
            return this._game;
        }

        getObjectMaker() {
            return this._objectMaker;
        }

        layout() {

            super.layout();

            if (!this._gameBooted) {
                return;
            }

            this._overlayLayer.resizeTo();

            const parent = this.getElement();
            const w = parent.clientWidth;
            const h = parent.clientHeight;

            this._game.scale.resize(w, h);
            this._gameScene.scale.resize(w, h);
            this._gameScene.getCamera().setSize(w, h);

            this.repaint();
        }

        getEditorViewerProvider(key: string) {

            switch (key) {
                case views.blocks.BlocksView.EDITOR_VIEWER_PROVIDER_KEY:
                    return this._blocksProvider;
                case views.outline.OutlineView.EDITOR_VIEWER_PROVIDER_KEY:
                    return this._outlineProvider;
                default:
                    break;
            }

            return null;
        }

        getOutlineProvider() {
            return this._outlineProvider;
        }

        refreshOutline() {
            this._outlineProvider.repaint();
        }

        private onGameBoot(): void {

            this._gameBooted = true;
            this.layout();

            this.refreshOutline();
        }

        repaint(): void {

            if (!this._gameBooted) {
                return;
            }

            this._game.loop.tick();

            this._overlayLayer.render();
        }
    }

}