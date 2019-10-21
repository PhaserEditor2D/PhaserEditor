declare namespace phasereditor2d.scene {
    import ide = colibri.ui.ide;
    const ICON_GROUP = "group";
    class ScenePlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): ScenePlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.scene.core {
    import core = colibri.core;
    const CONTENT_TYPE_SCENE = "Scene";
    class SceneContentTypeResolver extends core.ContentTypeResolver {
        constructor();
        computeContentType(file: core.io.FilePath): Promise<string>;
    }
}
declare namespace Phaser.Cameras.Scene2D {
    interface Camera {
        getScreenPoint(worldX: number, worldY: number): Phaser.Math.Vector2;
    }
}
declare namespace phasereditor2d.scene.ui {
}
declare namespace Phaser.GameObjects {
    interface DisplayList {
        getByEditorId(id: string): GameObject;
        visit(visitor: (obj: GameObject) => void): any;
        makeNewName(baseName: string): string;
    }
}
declare namespace phasereditor2d.scene.ui {
    function runObjectVisitor(obj: Phaser.GameObjects.GameObject, visitor: (obj: Phaser.GameObjects.GameObject) => void): void;
    function getByEditorId(list: Phaser.GameObjects.GameObject[], id: string): any;
}
declare namespace Phaser.GameObjects {
    interface EditorTexture {
        setEditorTexture(key: string, frame: string): void;
        getEditorTexture(): {
            key: string;
            frame: any;
        };
    }
    export interface GameObject {
        getEditorId(): string;
        setEditorId(id: string): void;
        getScreenBounds(camera: Phaser.Cameras.Scene2D.Camera): Phaser.Math.Vector2[];
        getEditorLabel(): string;
        setEditorLabel(label: string): void;
        getEditorScene(): phasereditor2d.scene.ui.GameScene;
        setEditorScene(scene: phasereditor2d.scene.ui.GameScene): void;
    }
    export interface Image extends EditorTexture {
    }
    export {};
}
declare namespace phasereditor2d.scene.ui {
    function getScreenBounds(sprite: Phaser.GameObjects.Image, camera: Phaser.Cameras.Scene2D.Camera): Phaser.Math.Vector2[];
}
declare namespace phasereditor2d.scene.ui {
    class GameScene extends Phaser.Scene {
        private _sceneType;
        private _inEditor;
        constructor(inEditor?: boolean);
        getSceneType(): json.SceneType;
        setSceneType(sceneType: json.SceneType): void;
        getCamera(): Phaser.Cameras.Scene2D.Camera;
        create(): void;
    }
}
declare namespace phasereditor2d.scene.ui {
    class SceneMaker {
        private _scene;
        constructor(scene: GameScene);
        createObject(objData: any): Phaser.GameObjects.GameObject;
        createContainerWithObjects(objects: Phaser.GameObjects.GameObject[]): Phaser.GameObjects.Container;
        createWithDropEvent_async(e: DragEvent, dropDataArray: any[]): Promise<Phaser.GameObjects.GameObject[]>;
    }
}
declare namespace phasereditor2d.scene.ui {
    import controls = colibri.ui.controls;
    import core = colibri.core;
    class SceneThumbnail implements controls.IImage {
        private _file;
        private _image;
        private _promise;
        constructor(file: core.io.FilePath);
        paint(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, center: boolean): void;
        paintFrame(context: CanvasRenderingContext2D, srcX: number, srcY: number, srcW: number, srcH: number, dstX: number, dstY: number, dstW: number, dstH: number): void;
        getWidth(): number;
        getHeight(): number;
        preload(): Promise<controls.PreloadResult>;
        private createImageElement;
    }
}
declare namespace phasereditor2d.scene.ui {
    import controls = colibri.ui.controls;
    import core = colibri.core;
    class SceneThumbnailCache extends core.io.FileContentCache<controls.IImage> {
        static _instance: SceneThumbnailCache;
        static getInstance(): SceneThumbnailCache;
        private constructor();
    }
}
declare namespace phasereditor2d.scene.ui.blocks {
    class SceneEditorBlocksCellRendererProvider extends pack.ui.viewers.AssetPackCellRendererProvider {
        constructor();
        getCellRenderer(element: any): colibri.ui.controls.viewers.ICellRenderer;
    }
}
declare namespace phasereditor2d.scene.ui.blocks {
    class SceneEditorBlocksContentProvider extends pack.ui.viewers.AssetPackContentProvider {
        getPackItems(): pack.core.AssetPackItem[];
        getRoots(input: any): any[];
        getSceneFiles(): colibri.core.io.FilePath[];
        getChildren(parent: any): any[];
    }
}
declare namespace phasereditor2d.scene.ui.blocks {
    class SceneEditorBlocksLabelProvider extends pack.ui.viewers.AssetPackLabelProvider {
        getLabel(obj: any): string;
    }
}
declare namespace phasereditor2d.scene.ui.blocks {
    import controls = colibri.ui.controls;
    class SceneEditorBlocksPropertyProvider extends controls.properties.PropertySectionProvider {
        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void;
    }
}
declare namespace phasereditor2d.scene.ui.blocks {
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    class SceneEditorBlocksProvider extends ide.EditorViewerProvider {
        preload(): Promise<void>;
        getContentProvider(): controls.viewers.ITreeContentProvider;
        getLabelProvider(): controls.viewers.ILabelProvider;
        getCellRendererProvider(): controls.viewers.ICellRendererProvider;
        getTreeViewerRenderer(viewer: controls.viewers.TreeViewer): SceneEditorBlocksTreeRendererProvider;
        getPropertySectionProvider(): controls.properties.PropertySectionProvider;
        getInput(): this;
    }
}
declare namespace phasereditor2d.scene.ui.blocks {
    import controls = colibri.ui.controls;
    const PREFAB_SECTION = "prefab";
    class SceneEditorBlocksTreeRendererProvider extends pack.ui.viewers.AssetPackTreeViewerRenderer {
        constructor(viewer: controls.viewers.TreeViewer);
    }
}
declare namespace phasereditor2d.scene.ui.editor {
    class ActionManager {
        private _editor;
        constructor(editor: SceneEditor);
        deleteObjects(): void;
        joinObjectsInContainer(): void;
    }
}
declare namespace phasereditor2d.scene.ui.editor {
    class CameraManager {
        private _editor;
        private _dragStartPoint;
        private _dragStartCameraScroll;
        constructor(editor: SceneEditor);
        private getCamera;
        private onMouseDown;
        private onMouseMove;
        private onMouseUp;
        private onWheel;
    }
}
declare namespace phasereditor2d.scene.ui.editor {
    class DropManager {
        private _editor;
        constructor(editor: SceneEditor);
        onDragDrop_async(e: DragEvent): Promise<void>;
        private onDragOver;
        private acceptsDropData;
        private acceptsDropDataArray;
    }
}
declare namespace phasereditor2d.scene.ui.editor {
    class OverlayLayer {
        private _editor;
        private _canvas;
        private _ctx;
        constructor(editor: SceneEditor);
        getCanvas(): HTMLCanvasElement;
        private resetContext;
        resizeTo(): void;
        render(): void;
        private renderSelection;
        private renderGrid;
    }
}
declare namespace phasereditor2d.scene.ui.editor {
    class SceneEditor extends colibri.ui.ide.FileEditor {
        private _blocksProvider;
        private _outlineProvider;
        private _propertyProvider;
        private _game;
        private _overlayLayer;
        private _gameCanvas;
        private _gameScene;
        private _sceneMaker;
        private _dropManager;
        private _cameraManager;
        private _selectionManager;
        private _actionManager;
        private _gameBooted;
        private _sceneRead;
        static getFactory(): colibri.ui.ide.EditorFactory;
        constructor();
        save(): Promise<void>;
        protected createPart(): void;
        setInput(file: colibri.core.io.FilePath): Promise<void>;
        private readScene;
        getSelectedGameObjects(): Phaser.GameObjects.GameObject[];
        getActionManager(): ActionManager;
        getSelectionManager(): SelectionManager;
        getOverlayLayer(): OverlayLayer;
        getGameCanvas(): HTMLCanvasElement;
        getGameScene(): GameScene;
        getGame(): Phaser.Game;
        getSceneMaker(): SceneMaker;
        layout(): void;
        getPropertyProvider(): properties.SceneEditorSectionProvider;
        getEditorViewerProvider(key: string): blocks.SceneEditorBlocksProvider | outline.SceneEditorOutlineProvider;
        getOutlineProvider(): outline.SceneEditorOutlineProvider;
        refreshOutline(): void;
        private onGameBoot;
        repaint(): void;
    }
}
declare namespace phasereditor2d.scene.ui.editor {
    class SelectionManager {
        private _editor;
        constructor(editor: SceneEditor);
        cleanSelection(): void;
        private updateOutlineSelection;
        private onMouseClick;
        hitTestOfActivePointer(): Phaser.GameObjects.GameObject[];
    }
}
declare namespace phasereditor2d.scene.ui.editor.commands {
    class SceneEditorCommands {
        static registerCommands(manager: colibri.ui.ide.commands.CommandManager): void;
    }
}
declare namespace phasereditor2d.scene.ui.editor.outline {
    import controls = colibri.ui.controls;
    class GameObjectCellRenderer implements controls.viewers.ICellRenderer {
        renderCell(args: controls.viewers.RenderCellArgs): void;
        cellHeight(args: colibri.ui.controls.viewers.RenderCellArgs): number;
        preload(obj: any): Promise<colibri.ui.controls.PreloadResult>;
    }
}
declare namespace phasereditor2d.scene.ui.editor.outline {
    import controls = colibri.ui.controls;
    class SceneEditorOutlineContentProvider implements controls.viewers.ITreeContentProvider {
        getRoots(input: any): any[];
        getChildren(parent: any): any[];
    }
}
declare namespace phasereditor2d.scene.ui.editor.outline {
    import controls = colibri.ui.controls;
    class SceneEditorOutlineLabelProvider implements controls.viewers.ILabelProvider {
        getLabel(obj: any): string;
    }
}
declare namespace phasereditor2d.scene.ui.editor.outline {
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    class SceneEditorOutlineProvider extends ide.EditorViewerProvider {
        private _editor;
        constructor(editor: SceneEditor);
        getContentProvider(): controls.viewers.ITreeContentProvider;
        getLabelProvider(): controls.viewers.ILabelProvider;
        getCellRendererProvider(): controls.viewers.ICellRendererProvider;
        getTreeViewerRenderer(viewer: controls.viewers.TreeViewer): controls.viewers.TreeViewerRenderer;
        getPropertySectionProvider(): controls.properties.PropertySectionProvider;
        getInput(): SceneEditor;
        preload(): Promise<void>;
        onViewerSelectionChanged(selection: any[]): void;
    }
}
declare namespace phasereditor2d.scene.ui.editor.outline {
    import controls = colibri.ui.controls;
    class SceneEditorOutlineRendererProvider implements controls.viewers.ICellRendererProvider {
        private _editor;
        private _assetRendererProvider;
        constructor(editor: SceneEditor);
        getCellRenderer(element: any): controls.viewers.ICellRenderer;
        preload(element: any): Promise<controls.PreloadResult>;
    }
}
declare namespace phasereditor2d.scene.ui.editor.properties {
    abstract class SceneSection<T> extends colibri.ui.controls.properties.PropertySection<T> {
    }
}
declare namespace phasereditor2d.scene.ui.editor.properties {
    import controls = colibri.ui.controls;
    class OriginSection extends SceneSection<Phaser.GameObjects.Image> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any, n: number): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.scene.ui.editor.properties {
    import controls = colibri.ui.controls;
    class SceneEditorSectionProvider extends controls.properties.PropertySectionProvider {
        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void;
    }
}
declare namespace phasereditor2d.scene.ui.editor.properties {
    import controls = colibri.ui.controls;
    class TextureSection extends SceneSection<Phaser.GameObjects.Image> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.scene.ui.editor.properties {
    class TransformSection extends SceneSection<Phaser.GameObjects.Image> {
        constructor(page: colibri.ui.controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any, n: number): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.scene.ui.editor.properties {
    import controls = colibri.ui.controls;
    class VariableSection extends SceneSection<Phaser.GameObjects.GameObject> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any, n: number): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.scene.ui.editor.undo {
    import ide = colibri.ui.ide;
    abstract class SceneEditorOperation extends ide.undo.Operation {
        protected _editor: SceneEditor;
        constructor(editor: SceneEditor);
    }
}
declare namespace phasereditor2d.scene.ui.editor.undo {
    class AddObjectsOperation extends SceneEditorOperation {
        private _dataList;
        constructor(editor: SceneEditor, objects: Phaser.GameObjects.GameObject[]);
        undo(): void;
        redo(): void;
        private updateEditor;
    }
}
declare namespace phasereditor2d.scene.ui.editor.undo {
    class JoinObjectsInContainerOperation extends SceneEditorOperation {
        private _containerId;
        private _objectsIdList;
        constructor(editor: SceneEditor, container: Phaser.GameObjects.Container);
        undo(): void;
        redo(): void;
        private updateEditor;
    }
}
declare namespace phasereditor2d.scene.ui.editor.undo {
    class RemoveObjectsOperation extends AddObjectsOperation {
        constructor(editor: SceneEditor, objects: Phaser.GameObjects.GameObject[]);
        undo(): void;
        redo(): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class ContainerComponent {
        static write(container: Phaser.GameObjects.Container, data: any): void;
        static read(container: Phaser.GameObjects.Container, data: any): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class ImageComponent {
        static write(sprite: Phaser.GameObjects.Image, data: any): void;
        static read(sprite: Phaser.GameObjects.Image, data: any): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class ObjectComponent {
        static write(sprite: Phaser.GameObjects.Image, data: any): void;
        static read(sprite: Phaser.GameObjects.Image, data: any): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
}
declare namespace Phaser.GameObjects {
    interface ReadWriteJSON {
        writeJSON(data: any): void;
        readJSON(data: any): void;
    }
    interface GameObject extends ReadWriteJSON {
    }
}
declare namespace phasereditor2d.scene.ui.json {
    type SceneType = "Scene" | "Prefab";
    type SceneData = {
        sceneType: SceneType;
        displayList: any[];
    };
}
declare namespace phasereditor2d.scene.ui.json {
    class SceneParser {
        private _scene;
        constructor(scene: GameScene);
        static isValidSceneDataFormat(data: SceneData): boolean;
        createScene(data: SceneData): void;
        createSceneCache_async(data: SceneData): Promise<void>;
        private updateSceneCacheWithObjectData_async;
        addToCache_async(data: pack.core.AssetPackItem | pack.core.AssetPackImageFrame): Promise<void>;
        createObject(data: any): Phaser.GameObjects.GameObject;
        static initSprite(sprite: Phaser.GameObjects.GameObject): void;
        static setNewId(sprite: Phaser.GameObjects.GameObject): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class SceneWriter {
        private _scene;
        constructor(scene: GameScene);
        toJSON(): SceneData;
        toString(): string;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class TextureComponent {
        static textureKey: string;
        static frameKey: string;
        static write(sprite: Phaser.GameObjects.Image, data: any): void;
        static read(sprite: Phaser.GameObjects.Image, data: any): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class TransformComponent {
        static write(sprite: Phaser.GameObjects.Image, data: any): void;
        static read(sprite: Phaser.GameObjects.Image, data: any): void;
    }
}
declare namespace phasereditor2d.scene.ui.json {
    class VariableComponent {
        static write(sprite: Phaser.GameObjects.Image, data: any): void;
        static read(sprite: Phaser.GameObjects.Image, data: any): void;
    }
}
declare namespace phasereditor2d.scene.ui.viewers {
    import controls = colibri.ui.controls;
    class SceneFileCellRenderer implements controls.viewers.ICellRenderer {
        renderCell(args: controls.viewers.RenderCellArgs): void;
        cellHeight(args: controls.viewers.RenderCellArgs): number;
        preload(obj: any): Promise<controls.PreloadResult>;
    }
}
//# sourceMappingURL=plugin.d.ts.map