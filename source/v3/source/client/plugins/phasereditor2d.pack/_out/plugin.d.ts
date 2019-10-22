declare namespace phasereditor2d.pack {
    import ide = colibri.ui.ide;
    const ICON_ASSET_PACK = "asset-pack";
    class AssetPackPlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): AssetPackPlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.pack.core {
    import core = colibri.core;
    const IMAGE_TYPE = "image";
    const ATLAS_TYPE = "atlas";
    const ATLAS_XML_TYPE = "atlasXML";
    const UNITY_ATLAS_TYPE = "unityAtlas";
    const MULTI_ATLAS_TYPE = "multiatlas";
    const SPRITESHEET_TYPE = "spritesheet";
    const ANIMATIONS_TYPE = "animations";
    const AUDIO_TYPE = "audio";
    const AUDIO_SPRITE_TYPE = "audioSprite";
    const BINARY_TYPE = "binary";
    const BITMAP_FONT_TYPE = "bitmapFont";
    const CSS_TYPE = "css";
    const GLSL_TYPE = "glsl";
    const HTML_TYPE = "html";
    const HTML_TEXTURE_TYPE = "htmlTexture";
    const JSON_TYPE = "json";
    const PLUGIN_TYPE = "plugin";
    const SCENE_FILE_TYPE = "sceneFile";
    const SCENE_PLUGIN_TYPE = "scenePlugin";
    const SCRIPT_TYPE = "script";
    const SVG_TYPE = "svg";
    const TEXT_TYPE = "text";
    const TILEMAP_CSV_TYPE = "tilemapCSV";
    const TILEMAP_IMPACT_TYPE = "tilemapImpact";
    const TILEMAP_TILED_JSON_TYPE = "tilemapTiledJSON";
    const VIDEO_TYPE = "video";
    const XML_TYPE = "xml";
    const TYPES: string[];
    class AssetPack {
        private _file;
        private _items;
        constructor(file: core.io.FilePath, content: string);
        static createFromFile(file: core.io.FilePath): Promise<AssetPack>;
        getItems(): AssetPackItem[];
        getFile(): core.io.FilePath;
    }
}
declare namespace phasereditor2d.pack.core {
    import core = colibri.core;
    const CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";
    class AssetPackContentTypeResolver extends core.ContentTypeResolver {
        constructor();
        computeContentType(file: core.io.FilePath): Promise<string>;
    }
}
declare namespace phasereditor2d.pack.core {
    import controls = colibri.ui.controls;
    class AssetPackImageFrame extends controls.ImageFrame {
        private _packItem;
        constructor(packItem: AssetPackItem, name: string, frameImage: controls.IImage, frameData: controls.FrameData);
        getPackItem(): AssetPackItem;
    }
}
declare namespace phasereditor2d.pack.core {
    class AssetPackItem {
        private _pack;
        private _data;
        private _editorData;
        constructor(pack: AssetPack, data: any);
        getEditorData(): any;
        getPack(): AssetPack;
        getKey(): string;
        getType(): string;
        getData(): any;
    }
}
declare namespace phasereditor2d.pack.core {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    class AssetPackUtils {
        static isAtlasType(type: string): boolean;
        static isAtlasPackItem(packItem: AssetPackItem): boolean;
        static isImageFrameContainer(packItem: AssetPackItem): boolean;
        static getImageFrames(packItem: AssetPackItem): AssetPackImageFrame[];
        static getImageFrameParser(packItem: AssetPackItem): parsers.ImageParser | parsers.AtlasParser | parsers.AtlasXMLParser | parsers.UnityAtlasParser | parsers.MultiAtlasParser | parsers.SpriteSheetParser;
        static preloadAssetPackItems(packItems: AssetPackItem[]): Promise<void>;
        static getAllPacks(): Promise<AssetPack[]>;
        static getFileFromPackUrl(url: string): io.FilePath;
        static getFileStringFromPackUrl(url: string): string;
        static getFileJSONFromPackUrl(url: string): any;
        static getFileXMLFromPackUrl(url: string): Document;
        static getImageFromPackUrl(url: string): controls.IImage;
    }
}
declare namespace phasereditor2d.pack.core {
    type FrameDataType = {
        "filename": string;
        "trimmed": boolean;
        "rotated": boolean;
        "frame": {
            "x": number;
            "y": number;
            "w": number;
            "h": number;
        };
        "spriteSourceSize": {
            "x": number;
            "y": number;
            "w": number;
            "h": number;
        };
        "sourceSize": {
            "w": number;
            "h": number;
        };
    };
}
declare namespace phasereditor2d.pack.core {
    import controls = colibri.ui.controls;
    class PackFinder {
        private static _packs;
        private static _loaded;
        private constructor();
        static preload(): Promise<controls.PreloadResult>;
        static getPacks(): AssetPack[];
        static findAssetPackItem(key: string): AssetPackItem;
        static getAssetPackItemOrFrame(key: string, frame: any): AssetPackItem | AssetPackImageFrame;
        static getAssetPackItemImage(key: string, frame: any): controls.IImage;
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    abstract class ImageFrameParser {
        private _packItem;
        constructor(packItem: AssetPackItem);
        protected setCachedFrames(frames: AssetPackImageFrame[]): void;
        protected getCachedFrames(): AssetPackImageFrame[];
        protected hasCachedFrames(): boolean;
        abstract addToPhaserCache(game: Phaser.Game): void;
        getPackItem(): AssetPackItem;
        preload(): Promise<controls.PreloadResult>;
        parse(): AssetPackImageFrame[];
        protected abstract preloadFrames(): Promise<controls.PreloadResult>;
        protected abstract parseFrames(): AssetPackImageFrame[];
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    abstract class BaseAtlasParser extends ImageFrameParser {
        constructor(packItem: AssetPackItem);
        addToPhaserCache(game: Phaser.Game): void;
        preloadFrames(): Promise<controls.PreloadResult>;
        protected abstract parseFrames2(frames: AssetPackImageFrame[], image: controls.IImage, atlas: string): any;
        parseFrames(): AssetPackImageFrame[];
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    class AtlasParser extends BaseAtlasParser {
        constructor(packItem: AssetPackItem);
        protected parseFrames2(imageFrames: AssetPackImageFrame[], image: controls.IImage, atlas: string): void;
        static buildFrameData(packItem: AssetPackItem, image: controls.IImage, frame: FrameDataType, index: number): AssetPackImageFrame;
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    class AtlasXMLParser extends BaseAtlasParser {
        constructor(packItem: AssetPackItem);
        addToPhaserCache(game: Phaser.Game): void;
        protected parseFrames2(imageFrames: AssetPackImageFrame[], image: controls.IImage, atlas: string): void;
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    class ImageParser extends ImageFrameParser {
        constructor(packItem: AssetPackItem);
        addToPhaserCache(game: Phaser.Game): void;
        protected preloadFrames(): Promise<controls.PreloadResult>;
        protected parseFrames(): AssetPackImageFrame[];
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    class MultiAtlasParser extends ImageFrameParser {
        constructor(packItem: AssetPackItem);
        addToPhaserCache(game: Phaser.Game): void;
        preloadFrames(): Promise<controls.PreloadResult>;
        parseFrames(): AssetPackImageFrame[];
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    class SpriteSheetParser extends ImageFrameParser {
        constructor(packItem: AssetPackItem);
        addToPhaserCache(game: Phaser.Game): void;
        preloadFrames(): Promise<controls.PreloadResult>;
        parseFrames(): AssetPackImageFrame[];
    }
}
declare namespace phasereditor2d.pack.core.parsers {
    import controls = colibri.ui.controls;
    class UnityAtlasParser extends BaseAtlasParser {
        addToPhaserCache(game: Phaser.Game): void;
        protected parseFrames2(imageFrames: AssetPackImageFrame[], image: controls.IImage, atlas: string): void;
        private addFrame;
    }
}
declare namespace phasereditor2d.pack.ui.editor {
    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    class AssetPackEditorFactory extends ide.EditorFactory {
        constructor();
        acceptInput(input: any): boolean;
        createEditor(): ide.EditorPart;
    }
    class AssetPackEditor extends ide.ViewerFileEditor {
        private _pack;
        private _outlineProvider;
        private _propertySectionProvider;
        constructor();
        static getFactory(): AssetPackEditorFactory;
        protected createViewer(): controls.viewers.TreeViewer;
        private updateContent;
        getPack(): core.AssetPack;
        setInput(file: io.FilePath): void;
        getEditorViewerProvider(key: string): ide.EditorViewerProvider;
        getPropertyProvider(): AssetPackEditorPropertySectionProvider;
    }
}
declare namespace phasereditor2d.pack.ui.viewers {
    import controls = colibri.ui.controls;
    abstract class AssetPackContentProvider implements controls.viewers.ITreeContentProvider {
        abstract getRoots(input: any): any[];
        getChildren(parent: any): any[];
    }
}
declare namespace phasereditor2d.pack.ui.editor {
    class AssetPackEditorContentProvider extends viewers.AssetPackContentProvider {
        private _editor;
        private _groupAtlasItems;
        constructor(editor: AssetPackEditor, groupAtlasItems: boolean);
        getPack(): core.AssetPack;
        getRoots(input: any): any[];
        getChildren(parent: any): any[];
    }
}
declare namespace phasereditor2d.pack.ui.editor {
    class AssetPackEditorOutlineContentProvider extends AssetPackEditorContentProvider {
        constructor(editor: AssetPackEditor);
        getRoots(): string[];
    }
}
declare namespace phasereditor2d.pack.ui.editor {
    import ide = colibri.ui.ide;
    class AssetPackEditorOutlineProvider extends ide.EditorViewerProvider {
        private _editor;
        constructor(editor: AssetPackEditor);
        getContentProvider(): colibri.ui.controls.viewers.ITreeContentProvider;
        getLabelProvider(): colibri.ui.controls.viewers.ILabelProvider;
        getCellRendererProvider(): colibri.ui.controls.viewers.ICellRendererProvider;
        getTreeViewerRenderer(viewer: colibri.ui.controls.viewers.TreeViewer): colibri.ui.controls.viewers.TreeViewerRenderer;
        getPropertySectionProvider(): colibri.ui.controls.properties.PropertySectionProvider;
        getInput(): any;
        preload(): Promise<void>;
        onViewerSelectionChanged(selection: any[]): void;
    }
}
declare namespace phasereditor2d.pack.ui.editor {
    import controls = colibri.ui.controls;
    class AssetPackEditorPropertySectionProvider extends controls.properties.PropertySectionProvider {
        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void;
    }
}
declare namespace phasereditor2d.pack.ui.properties {
    import controls = colibri.ui.controls;
    class AssetPackItemSection extends controls.properties.PropertySection<core.AssetPackItem> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.pack.ui.properties {
    import controls = colibri.ui.controls;
    class ImageSection extends controls.properties.PropertySection<core.AssetPackItem> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.pack.ui.properties {
    import controls = colibri.ui.controls;
    class ManyImageSection extends controls.properties.PropertySection<any> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        private getImageFrames;
        canEdit(obj: any, n: number): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.pack.ui.viewers {
    import controls = colibri.ui.controls;
    class AssetPackCellRendererProvider implements controls.viewers.ICellRendererProvider {
        private _layout;
        constructor(layout: "grid" | "tree");
        getCellRenderer(element: any): controls.viewers.ICellRenderer;
        private getIconRenderer;
        preload(element: any): Promise<controls.PreloadResult>;
    }
}
declare namespace phasereditor2d.pack.ui.viewers {
    import controls = colibri.ui.controls;
    class AssetPackLabelProvider implements controls.viewers.ILabelProvider {
        getLabel(obj: any): string;
    }
}
declare namespace phasereditor2d.pack.ui.viewers {
    import controls = colibri.ui.controls;
    class AssetPackTreeViewerRenderer extends controls.viewers.GridTreeViewerRenderer {
        constructor(viewer: controls.viewers.TreeViewer, flat: boolean);
        renderCellBack(args: controls.viewers.RenderCellArgs, selected: boolean, isLastChild: boolean): void;
        protected isParent(obj: any): boolean;
        protected isChild(obj: any): boolean;
    }
}
declare namespace phasereditor2d.pack.ui.viewers {
    import controls = colibri.ui.controls;
    class ImageFrameContainerIconCellRenderer implements controls.viewers.ICellRenderer {
        renderCell(args: controls.viewers.RenderCellArgs): void;
        cellHeight(args: controls.viewers.RenderCellArgs): number;
        preload(obj: any): Promise<controls.PreloadResult>;
    }
}
declare namespace phasereditor2d.pack.ui.viewers {
    import controls = colibri.ui.controls;
    class ImageAssetPackItemCellRenderer extends controls.viewers.ImageCellRenderer {
        getImage(obj: any): controls.IImage;
    }
}
//# sourceMappingURL=plugin.d.ts.map