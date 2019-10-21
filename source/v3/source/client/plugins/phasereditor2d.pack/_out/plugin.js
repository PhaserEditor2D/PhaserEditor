var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ide = colibri.ui.ide;
        pack.ICON_ASSET_PACK = "asset-pack";
        class AssetPackPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.pack");
            }
            static getInstance() {
                return this._instance;
            }
            registerExtensions(reg) {
                // icons loader
                reg.addExtension(ide.IconLoaderExtension.POINT_ID, ide.IconLoaderExtension.withPluginFiles(this, [
                    pack.ICON_ASSET_PACK
                ]));
                // content type resolvers
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.AssetPackContentTypeResolver", [new pack.core.AssetPackContentTypeResolver()], 5));
                // content type icons
                reg.addExtension(ide.ContentTypeIconExtension.POINT_ID, ide.ContentTypeIconExtension.withPluginIcons(this, [
                    {
                        iconName: pack.ICON_ASSET_PACK,
                        contentType: pack.core.CONTENT_TYPE_ASSET_PACK
                    }
                ]));
                // project resources preloader
                reg.addExtension(ide.PreloadProjectResourcesExtension.POINT_ID, new ide.PreloadProjectResourcesExtension("phasereditor2d.pack.PreloadProjectResourcesExtension", () => pack.core.PackFinder.preload()));
                // editors
                reg.addExtension(ide.EditorExtension.POINT_ID, new ide.EditorExtension("phasereditor2d.pack.EditorExtension", [
                    pack.ui.editor.AssetPackEditor.getFactory()
                ]));
            }
        }
        AssetPackPlugin._instance = new AssetPackPlugin();
        pack.AssetPackPlugin = AssetPackPlugin;
        ide.Workbench.getWorkbench().addPlugin(AssetPackPlugin.getInstance());
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_1) {
            var ide = colibri.ui.ide;
            core_1.IMAGE_TYPE = "image";
            core_1.ATLAS_TYPE = "atlas";
            core_1.ATLAS_XML_TYPE = "atlasXML";
            core_1.UNITY_ATLAS_TYPE = "unityAtlas";
            core_1.MULTI_ATLAS_TYPE = "multiatlas";
            core_1.SPRITESHEET_TYPE = "spritesheet";
            core_1.ANIMATIONS_TYPE = "animations";
            core_1.AUDIO_TYPE = "audio";
            core_1.AUDIO_SPRITE_TYPE = "audioSprite";
            core_1.BINARY_TYPE = "binary";
            core_1.BITMAP_FONT_TYPE = "bitmapFont";
            core_1.CSS_TYPE = "css";
            core_1.GLSL_TYPE = "glsl";
            core_1.HTML_TYPE = "html";
            core_1.HTML_TEXTURE_TYPE = "htmlTexture";
            core_1.JSON_TYPE = "json";
            core_1.PLUGIN_TYPE = "plugin";
            core_1.SCENE_FILE_TYPE = "sceneFile";
            core_1.SCENE_PLUGIN_TYPE = "scenePlugin";
            core_1.SCRIPT_TYPE = "script";
            core_1.SVG_TYPE = "svg";
            core_1.TEXT_TYPE = "text";
            core_1.TILEMAP_CSV_TYPE = "tilemapCSV";
            core_1.TILEMAP_IMPACT_TYPE = "tilemapImpact";
            core_1.TILEMAP_TILED_JSON_TYPE = "tilemapTiledJSON";
            core_1.VIDEO_TYPE = "video";
            core_1.XML_TYPE = "xml";
            core_1.TYPES = [
                core_1.IMAGE_TYPE,
                core_1.SVG_TYPE,
                core_1.ATLAS_TYPE,
                core_1.ATLAS_XML_TYPE,
                core_1.UNITY_ATLAS_TYPE,
                core_1.MULTI_ATLAS_TYPE,
                core_1.SPRITESHEET_TYPE,
                core_1.ANIMATIONS_TYPE,
                core_1.BITMAP_FONT_TYPE,
                core_1.TILEMAP_CSV_TYPE,
                core_1.TILEMAP_IMPACT_TYPE,
                core_1.TILEMAP_TILED_JSON_TYPE,
                core_1.PLUGIN_TYPE,
                core_1.SCENE_FILE_TYPE,
                core_1.SCENE_PLUGIN_TYPE,
                core_1.SCRIPT_TYPE,
                core_1.AUDIO_TYPE,
                core_1.AUDIO_SPRITE_TYPE,
                core_1.VIDEO_TYPE,
                core_1.TEXT_TYPE,
                core_1.CSS_TYPE,
                core_1.GLSL_TYPE,
                core_1.HTML_TYPE,
                core_1.HTML_TEXTURE_TYPE,
                core_1.BINARY_TYPE,
                core_1.JSON_TYPE,
                core_1.XML_TYPE
            ];
            class AssetPack {
                constructor(file, content) {
                    this._file = file;
                    this._items = [];
                    if (content) {
                        try {
                            const data = JSON.parse(content);
                            for (const sectionId in data) {
                                const sectionData = data[sectionId];
                                const filesData = sectionData["files"];
                                if (filesData) {
                                    for (const fileData of filesData) {
                                        const item = new core_1.AssetPackItem(this, fileData);
                                        this._items.push(item);
                                    }
                                }
                            }
                        }
                        catch (e) {
                            console.error(e);
                            alert(e.message);
                        }
                    }
                }
                static async createFromFile(file) {
                    const content = await ide.FileUtils.preloadAndGetFileString(file);
                    return new AssetPack(file, content);
                }
                getItems() {
                    return this._items;
                }
                getFile() {
                    return this._file;
                }
            }
            core_1.AssetPack = AssetPack;
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_2) {
            var ide = colibri.ui.ide;
            var core = colibri.core;
            core_2.CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";
            class AssetPackContentTypeResolver extends core.ContentTypeResolver {
                constructor() {
                    super("phasereditor2d.pack.core.AssetPackContentTypeResolver");
                }
                async computeContentType(file) {
                    if (file.getExtension() === "json") {
                        const content = await ide.FileUtils.preloadAndGetFileString(file);
                        if (content !== null) {
                            try {
                                const data = JSON.parse(content);
                                const meta = data["meta"];
                                if (meta["contentType"] === "Phaser v3 Asset Pack") {
                                    return core_2.CONTENT_TYPE_ASSET_PACK;
                                }
                            }
                            catch (e) {
                            }
                        }
                    }
                    return core.CONTENT_TYPE_ANY;
                }
            }
            core_2.AssetPackContentTypeResolver = AssetPackContentTypeResolver;
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var controls = colibri.ui.controls;
            class AssetPackImageFrame extends controls.ImageFrame {
                constructor(packItem, name, frameImage, frameData) {
                    super(name, frameImage, frameData);
                    this._packItem = packItem;
                }
                getPackItem() {
                    return this._packItem;
                }
            }
            core.AssetPackImageFrame = AssetPackImageFrame;
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_1) {
        var core;
        (function (core) {
            class AssetPackItem {
                constructor(pack, data) {
                    this._pack = pack;
                    this._data = data;
                    this._editorData = {};
                }
                getEditorData() {
                    return this._editorData;
                }
                getPack() {
                    return this._pack;
                }
                getKey() {
                    return this._data["key"];
                }
                getType() {
                    return this._data["type"];
                }
                getData() {
                    return this._data;
                }
            }
            core.AssetPackItem = AssetPackItem;
        })(core = pack_1.core || (pack_1.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_2) {
        var core;
        (function (core) {
            var ide = colibri.ui.ide;
            const IMAGE_FRAME_CONTAINER_TYPES = new Set([
                core.IMAGE_TYPE,
                core.MULTI_ATLAS_TYPE,
                core.ATLAS_TYPE,
                core.UNITY_ATLAS_TYPE,
                core.ATLAS_XML_TYPE,
                core.SPRITESHEET_TYPE
            ]);
            const ATLAS_TYPES = new Set([
                core.MULTI_ATLAS_TYPE,
                core.ATLAS_TYPE,
                core.UNITY_ATLAS_TYPE,
                core.ATLAS_XML_TYPE,
            ]);
            class AssetPackUtils {
                static isAtlasPackItem(packItem) {
                    return ATLAS_TYPES.has(packItem.getType());
                }
                static isImageFrameContainer(packItem) {
                    return IMAGE_FRAME_CONTAINER_TYPES.has(packItem.getType());
                }
                static getImageFrames(packItem) {
                    const parser = this.getImageFrameParser(packItem);
                    if (parser) {
                        return parser.parse();
                    }
                    return [];
                }
                static getImageFrameParser(packItem) {
                    switch (packItem.getType()) {
                        case core.IMAGE_TYPE:
                            return new core.parsers.ImageParser(packItem);
                        case core.ATLAS_TYPE:
                            return new core.parsers.AtlasParser(packItem);
                        case core.ATLAS_XML_TYPE:
                            return new core.parsers.AtlasXMLParser(packItem);
                        case core.UNITY_ATLAS_TYPE:
                            return new core.parsers.UnityAtlasParser(packItem);
                        case core.MULTI_ATLAS_TYPE:
                            return new core.parsers.MultiAtlasParser(packItem);
                        case core.SPRITESHEET_TYPE:
                            return new core.parsers.SpriteSheetParser(packItem);
                        default:
                            break;
                    }
                    return null;
                }
                static async preloadAssetPackItems(packItems) {
                    for (const item of packItems) {
                        if (this.isImageFrameContainer(item)) {
                            const parser = this.getImageFrameParser(item);
                            await parser.preload();
                        }
                    }
                }
                static async getAllPacks() {
                    const files = await ide.FileUtils.getFilesWithContentType(core.CONTENT_TYPE_ASSET_PACK);
                    const packs = [];
                    for (const file of files) {
                        const pack = await core.AssetPack.createFromFile(file);
                        packs.push(pack);
                    }
                    return packs;
                }
                static getFileFromPackUrl(url) {
                    return ide.FileUtils.getFileFromPath(url);
                }
                static getFileStringFromPackUrl(url) {
                    const file = ide.FileUtils.getFileFromPath(url);
                    const str = ide.FileUtils.getFileString(file);
                    return str;
                }
                static getFileJSONFromPackUrl(url) {
                    const str = this.getFileStringFromPackUrl(url);
                    return JSON.parse(str);
                }
                static getFileXMLFromPackUrl(url) {
                    const str = this.getFileStringFromPackUrl(url);
                    const parser = new DOMParser();
                    return parser.parseFromString(str, "text/xml");
                }
                static getImageFromPackUrl(url) {
                    const file = this.getFileFromPackUrl(url);
                    if (file) {
                        return ide.Workbench.getWorkbench().getFileImage(file);
                    }
                    return null;
                }
            }
            core.AssetPackUtils = AssetPackUtils;
        })(core = pack_2.core || (pack_2.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_3) {
        var core;
        (function (core_3) {
            var controls = colibri.ui.controls;
            class PackFinder {
                constructor() {
                }
                static async preload() {
                    if (this._loaded) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                    this._packs = await core_3.AssetPackUtils.getAllPacks();
                    const items = this._packs.flatMap(pack => pack.getItems());
                    await core_3.AssetPackUtils.preloadAssetPackItems(items);
                    return controls.Controls.resolveResourceLoaded();
                }
                static getPacks() {
                    return this._packs;
                }
                static findAssetPackItem(key) {
                    return this._packs
                        .flatMap(pack => pack.getItems())
                        .find(item => item.getKey() === key);
                }
                static getAssetPackItemOrFrame(key, frame) {
                    let item = this.findAssetPackItem(key);
                    if (!item) {
                        return null;
                    }
                    if (item.getType() === core_3.IMAGE_TYPE) {
                        if (frame === null || frame === undefined) {
                            return item;
                        }
                        return null;
                    }
                    else if (core_3.AssetPackUtils.isImageFrameContainer(item)) {
                        const frames = core_3.AssetPackUtils.getImageFrames(item);
                        const imageFrame = frames.find(imageFrame => imageFrame.getName() === frame);
                        return imageFrame;
                    }
                    return item;
                }
                static getAssetPackItemImage(key, frame) {
                    const asset = this.getAssetPackItemOrFrame(key, frame);
                    if (asset instanceof core_3.AssetPackItem && asset.getType() === core_3.IMAGE_TYPE) {
                        return core_3.AssetPackUtils.getImageFromPackUrl(asset.getData().url);
                    }
                    else if (asset instanceof core_3.AssetPackImageFrame) {
                        return asset;
                    }
                    return new controls.ImageWrapper(null);
                }
            }
            PackFinder._packs = [];
            PackFinder._loaded = false;
            core_3.PackFinder = PackFinder;
        })(core = pack_3.core || (pack_3.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                class ImageFrameParser {
                    constructor(packItem) {
                        this._packItem = packItem;
                    }
                    setCachedFrames(frames) {
                        this._packItem.getEditorData()["__frames_cache"] = frames;
                    }
                    getCachedFrames() {
                        return this._packItem.getEditorData()["__frames_cache"];
                    }
                    hasCachedFrames() {
                        return "__frames_cache" in this._packItem.getEditorData();
                    }
                    getPackItem() {
                        return this._packItem;
                    }
                    async preload() {
                        if (this.hasCachedFrames()) {
                            return controls.Controls.resolveNothingLoaded();
                        }
                        return this.preloadFrames();
                    }
                    parse() {
                        if (this.hasCachedFrames()) {
                            return this.getCachedFrames();
                        }
                        const frames = this.parseFrames();
                        this.setCachedFrames(frames);
                        return frames;
                    }
                }
                parsers.ImageFrameParser = ImageFrameParser;
            })(parsers = core.parsers || (core.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./ImageFrameParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_4) {
            var parsers;
            (function (parsers) {
                var ide = colibri.ui.ide;
                class BaseAtlasParser extends parsers.ImageFrameParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const atlasURL = item.getData().atlasURL;
                            const atlasData = core_4.AssetPackUtils.getFileJSONFromPackUrl(atlasURL);
                            const textureURL = item.getData().textureURL;
                            const image = core_4.AssetPackUtils.getImageFromPackUrl(textureURL);
                            game.textures.addAtlas(item.getKey(), image.getImageElement(), atlasData);
                        }
                    }
                    async preloadFrames() {
                        const data = this.getPackItem().getData();
                        const dataFile = core_4.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                        let result1 = await ide.FileUtils.preloadFileString(dataFile);
                        const imageFile = core_4.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                        const image = ide.FileUtils.getImage(imageFile);
                        let result2 = await image.preload();
                        return Math.max(result1, result2);
                    }
                    parseFrames() {
                        if (this.hasCachedFrames()) {
                            return this.getCachedFrames();
                        }
                        const list = [];
                        const data = this.getPackItem().getData();
                        const dataFile = core_4.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                        const imageFile = core_4.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                        const image = ide.FileUtils.getImage(imageFile);
                        if (dataFile) {
                            const str = ide.FileUtils.getFileString(dataFile);
                            try {
                                this.parseFrames2(list, image, str);
                            }
                            catch (e) {
                                console.error(e);
                            }
                        }
                        return list;
                    }
                }
                parsers.BaseAtlasParser = BaseAtlasParser;
            })(parsers = core_4.parsers || (core_4.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                class AtlasParser extends parsers.BaseAtlasParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    parseFrames2(imageFrames, image, atlas) {
                        try {
                            const data = JSON.parse(atlas);
                            if (Array.isArray(data.frames)) {
                                for (const frame of data.frames) {
                                    const frameData = AtlasParser.buildFrameData(this.getPackItem(), image, frame, imageFrames.length);
                                    imageFrames.push(frameData);
                                }
                            }
                            else {
                                for (const name in data.frames) {
                                    const frame = data.frames[name];
                                    frame.filename = name;
                                    const frameData = AtlasParser.buildFrameData(this.getPackItem(), image, frame, imageFrames.length);
                                    imageFrames.push(frameData);
                                }
                            }
                        }
                        catch (e) {
                            console.error(e);
                        }
                    }
                    static buildFrameData(packItem, image, frame, index) {
                        const src = new controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
                        const dst = new controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
                        const srcSize = new controls.Point(frame.sourceSize.w, frame.sourceSize.h);
                        const frameData = new controls.FrameData(index, src, dst, srcSize);
                        return new core.AssetPackImageFrame(packItem, frame.filename, image, frameData);
                    }
                }
                parsers.AtlasParser = AtlasParser;
            })(parsers = core.parsers || (core.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_5) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                class AtlasXMLParser extends parsers.BaseAtlasParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const atlasURL = item.getData().atlasURL;
                            const atlasData = core_5.AssetPackUtils.getFileXMLFromPackUrl(atlasURL);
                            const textureURL = item.getData().textureURL;
                            const image = core_5.AssetPackUtils.getImageFromPackUrl(textureURL);
                            game.textures.addAtlasXML(item.getKey(), image.getImageElement(), atlasData);
                        }
                    }
                    parseFrames2(imageFrames, image, atlas) {
                        try {
                            const parser = new DOMParser();
                            const data = parser.parseFromString(atlas, "text/xml");
                            const elements = data.getElementsByTagName("SubTexture");
                            for (let i = 0; i < elements.length; i++) {
                                const elem = elements.item(i);
                                const name = elem.getAttribute("name");
                                const frameX = Number.parseInt(elem.getAttribute("x"));
                                const frameY = Number.parseInt(elem.getAttribute("y"));
                                const frameW = Number.parseInt(elem.getAttribute("width"));
                                const frameH = Number.parseInt(elem.getAttribute("height"));
                                let spriteX = frameX;
                                let spriteY = frameY;
                                let spriteW = frameW;
                                let spriteH = frameH;
                                if (elem.hasAttribute("frameX")) {
                                    spriteX = Number.parseInt(elem.getAttribute("frameX"));
                                    spriteY = Number.parseInt(elem.getAttribute("frameY"));
                                    spriteW = Number.parseInt(elem.getAttribute("frameWidth"));
                                    spriteH = Number.parseInt(elem.getAttribute("frameHeight"));
                                }
                                const fd = new controls.FrameData(i, new controls.Rect(frameX, frameY, frameW, frameH), new controls.Rect(spriteX, spriteY, spriteW, spriteH), new controls.Point(frameW, frameH));
                                imageFrames.push(new core_5.AssetPackImageFrame(this.getPackItem(), name, image, fd));
                            }
                        }
                        catch (e) {
                            console.error(e);
                        }
                    }
                }
                parsers.AtlasXMLParser = AtlasXMLParser;
            })(parsers = core_5.parsers || (core_5.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                class ImageParser extends parsers.ImageFrameParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const url = item.getData().url;
                            const image = core.AssetPackUtils.getImageFromPackUrl(url);
                            game.textures.addImage(item.getKey(), image.getImageElement());
                        }
                    }
                    preloadFrames() {
                        const url = this.getPackItem().getData().url;
                        const img = core.AssetPackUtils.getImageFromPackUrl(url);
                        return img.preload();
                    }
                    parseFrames() {
                        const url = this.getPackItem().getData().url;
                        const img = core.AssetPackUtils.getImageFromPackUrl(url);
                        const fd = new controls.FrameData(0, new controls.Rect(0, 0, img.getWidth(), img.getHeight()), new controls.Rect(0, 0, img.getWidth(), img.getHeight()), new controls.Point(img.getWidth(), img.getWidth()));
                        return [new core.AssetPackImageFrame(this.getPackItem(), this.getPackItem().getKey(), img, fd)];
                    }
                }
                parsers.ImageParser = ImageParser;
            })(parsers = core.parsers || (core.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_6) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class MultiAtlasParser extends parsers.ImageFrameParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const packItemData = item.getData();
                            const atlasDataFile = core_6.AssetPackUtils.getFileFromPackUrl(packItemData.url);
                            const atlasData = core_6.AssetPackUtils.getFileJSONFromPackUrl(packItemData.url);
                            const images = [];
                            const jsonArrayData = [];
                            for (const textureData of atlasData.textures) {
                                const imageName = textureData.image;
                                const imageFile = atlasDataFile.getSibling(imageName);
                                const image = ide.FileUtils.getImage(imageFile);
                                images.push(image.getImageElement());
                                jsonArrayData.push(textureData);
                            }
                            game.textures.addAtlasJSONArray(this.getPackItem().getKey(), images, jsonArrayData);
                        }
                    }
                    async preloadFrames() {
                        const data = this.getPackItem().getData();
                        const dataFile = core_6.AssetPackUtils.getFileFromPackUrl(data.url);
                        if (dataFile) {
                            let result = await ide.FileUtils.preloadFileString(dataFile);
                            const str = ide.FileUtils.getFileString(dataFile);
                            try {
                                const data = JSON.parse(str);
                                if (data.textures) {
                                    for (const texture of data.textures) {
                                        const imageName = texture.image;
                                        const imageFile = dataFile.getSibling(imageName);
                                        if (imageFile) {
                                            const image = ide.Workbench.getWorkbench().getFileImage(imageFile);
                                            const result2 = await image.preload();
                                            result = Math.max(result, result2);
                                        }
                                    }
                                }
                            }
                            catch (e) {
                            }
                            return result;
                        }
                        return controls.Controls.resolveNothingLoaded();
                    }
                    parseFrames() {
                        const list = [];
                        const data = this.getPackItem().getData();
                        const dataFile = core_6.AssetPackUtils.getFileFromPackUrl(data.url);
                        if (dataFile) {
                            const str = ide.FileUtils.getFileString(dataFile);
                            try {
                                const data = JSON.parse(str);
                                if (data.textures) {
                                    for (const textureData of data.textures) {
                                        const imageName = textureData.image;
                                        const imageFile = dataFile.getSibling(imageName);
                                        const image = ide.FileUtils.getImage(imageFile);
                                        for (const frame of textureData.frames) {
                                            const frameData = parsers.AtlasParser.buildFrameData(this.getPackItem(), image, frame, list.length);
                                            list.push(frameData);
                                        }
                                    }
                                }
                            }
                            catch (e) {
                                console.error(e);
                            }
                        }
                        return list;
                    }
                }
                parsers.MultiAtlasParser = MultiAtlasParser;
            })(parsers = core_6.parsers || (core_6.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_7) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class SpriteSheetParser extends parsers.ImageFrameParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const data = item.getData();
                            const image = core_7.AssetPackUtils.getImageFromPackUrl(data.url);
                            game.textures.addSpriteSheet(item.getKey(), image.getImageElement(), data.frameConfig);
                        }
                    }
                    async preloadFrames() {
                        const data = this.getPackItem().getData();
                        const imageFile = core_7.AssetPackUtils.getFileFromPackUrl(data.url);
                        const image = ide.FileUtils.getImage(imageFile);
                        return await image.preload();
                    }
                    parseFrames() {
                        const frames = [];
                        const data = this.getPackItem().getData();
                        const imageFile = core_7.AssetPackUtils.getFileFromPackUrl(data.url);
                        const image = ide.FileUtils.getImage(imageFile);
                        const w = data.frameConfig.frameWidth;
                        const h = data.frameConfig.frameHeight;
                        const margin = data.frameConfig.margin || 0;
                        const spacing = data.frameConfig.spacing || 0;
                        const startFrame = data.frameConfig.startFrame || 0;
                        const endFrame = data.frameConfig.endFrame || -1;
                        if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
                            // invalid values
                            return frames;
                        }
                        const start = startFrame < 0 ? 0 : startFrame;
                        const end = endFrame < 0 ? Number.MAX_VALUE : endFrame;
                        let i = 0;
                        let row = 0;
                        let column = 0;
                        let x = margin;
                        let y = margin;
                        while (true) {
                            if (i > end || y >= image.getHeight() || i > 50) {
                                break;
                            }
                            if (i >= start) {
                                if (x + w <= image.getWidth() && y + h <= image.getHeight()) {
                                    // FrameModel frame = new FrameModel(this, i, row, column, new Rectangle(x, y, w, h));
                                    // list.add(frame);
                                    const fd = new controls.FrameData(i, new controls.Rect(x, y, w, h), new controls.Rect(0, 0, w, h), new controls.Point(w, h));
                                    frames.push(new core_7.AssetPackImageFrame(this.getPackItem(), i.toString(), image, fd));
                                }
                            }
                            column++;
                            x += w + spacing;
                            if (x >= image.getWidth()) {
                                x = margin;
                                y += h + spacing;
                                column = 0;
                                row++;
                            }
                            i++;
                        }
                        return frames;
                    }
                }
                parsers.SpriteSheetParser = SpriteSheetParser;
            })(parsers = core_7.parsers || (core_7.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var parsers;
            (function (parsers) {
                var controls = colibri.ui.controls;
                class UnityAtlasParser extends parsers.BaseAtlasParser {
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const atlasURL = item.getData().atlasURL;
                            const atlasData = core.AssetPackUtils.getFileStringFromPackUrl(atlasURL);
                            const textureURL = item.getData().textureURL;
                            const image = core.AssetPackUtils.getImageFromPackUrl(textureURL);
                            game.textures.addUnityAtlas(item.getKey(), image.getImageElement(), atlasData);
                        }
                    }
                    parseFrames2(imageFrames, image, atlas) {
                        // Taken from Phaser code.
                        const data = atlas.split('\n');
                        const lineRegExp = /^[ ]*(- )*(\w+)+[: ]+(.*)/;
                        let prevSprite = '';
                        let currentSprite = '';
                        let rect = { x: 0, y: 0, width: 0, height: 0 };
                        // const pivot = { x: 0, y: 0 };
                        // const border = { x: 0, y: 0, z: 0, w: 0 };
                        for (let i = 0; i < data.length; i++) {
                            const results = data[i].match(lineRegExp);
                            if (!results) {
                                continue;
                            }
                            const isList = (results[1] === '- ');
                            const key = results[2];
                            const value = results[3];
                            if (isList) {
                                if (currentSprite !== prevSprite) {
                                    this.addFrame(image, imageFrames, currentSprite, rect);
                                    prevSprite = currentSprite;
                                }
                                rect = { x: 0, y: 0, width: 0, height: 0 };
                            }
                            if (key === 'name') {
                                //  Start new list
                                currentSprite = value;
                                continue;
                            }
                            switch (key) {
                                case 'x':
                                case 'y':
                                case 'width':
                                case 'height':
                                    rect[key] = parseInt(value, 10);
                                    break;
                                // case 'pivot':
                                //     pivot = eval('const obj = ' + value);
                                //     break;
                                // case 'border':
                                //     border = eval('const obj = ' + value);
                                //     break;
                            }
                        }
                        if (currentSprite !== prevSprite) {
                            this.addFrame(image, imageFrames, currentSprite, rect);
                        }
                    }
                    addFrame(image, imageFrames, spriteName, rect) {
                        const src = new controls.Rect(rect.x, rect.y, rect.width, rect.height);
                        src.y = image.getHeight() - src.y - src.h;
                        const dst = new controls.Rect(0, 0, rect.width, rect.height);
                        const srcSize = new controls.Point(rect.width, rect.height);
                        const fd = new controls.FrameData(imageFrames.length, src, dst, srcSize);
                        imageFrames.push(new core.AssetPackImageFrame(this.getPackItem(), spriteName, image, fd));
                    }
                }
                parsers.UnityAtlasParser = UnityAtlasParser;
            })(parsers = core.parsers || (core.parsers = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/*

TextureImporter:
  spritePivot: {x: .5, y: .5}
  spriteBorder: {x: 0, y: 0, z: 0, w: 0}
  spritePixelsToUnits: 100
  spriteSheet:
    sprites:
    - name: asteroids_0
      rect:
        serializedVersion: 2
        x: 5
        y: 328
        width: 65
        height: 82
      alignment: 0
      pivot: {x: 0, y: 0}
      border: {x: 0, y: 0, z: 0, w: 0}
    - name: asteroids_1
      rect:
        serializedVersion: 2
        x: 80
        y: 322
        width: 53
        height: 88
      alignment: 0
      pivot: {x: 0, y: 0}
      border: {x: 0, y: 0, z: 0, w: 0}
  spritePackingTag: Asteroids

  */ 
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var editor;
            (function (editor) {
                var ide = colibri.ui.ide;
                var controls = colibri.ui.controls;
                var io = colibri.core.io;
                class AssetPackEditorFactory extends ide.EditorFactory {
                    constructor() {
                        super("phasereditor2d.AssetPackEditorFactory");
                    }
                    acceptInput(input) {
                        if (input instanceof io.FilePath) {
                            const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                            return contentType === pack.core.CONTENT_TYPE_ASSET_PACK;
                        }
                        return false;
                    }
                    createEditor() {
                        return new AssetPackEditor();
                    }
                }
                editor.AssetPackEditorFactory = AssetPackEditorFactory;
                class AssetPackEditor extends ide.ViewerFileEditor {
                    constructor() {
                        super("phasereditor2d.AssetPackEditor");
                        this._outlineProvider = new editor.AssetPackEditorOutlineProvider(this);
                        this.addClass("AssetPackEditor");
                    }
                    static getFactory() {
                        return new AssetPackEditorFactory();
                    }
                    createViewer() {
                        const viewer = new controls.viewers.TreeViewer();
                        viewer.setContentProvider(this._contentProvider = new editor.AssetPackEditorContentProvider(this));
                        viewer.setLabelProvider(new ui.viewers.AssetPackLabelProvider());
                        viewer.setCellRendererProvider(new ui.viewers.AssetPackCellRendererProvider("grid"));
                        viewer.setTreeRenderer(new ui.viewers.AssetPackTreeViewerRenderer(viewer, true));
                        viewer.setInput(this);
                        this.updateContent();
                        return viewer;
                    }
                    async updateContent() {
                        const file = this.getInput();
                        if (!file) {
                            return;
                        }
                        const content = await ide.FileUtils.preloadAndGetFileString(file);
                        this._pack = new pack.core.AssetPack(file, content);
                        this.getViewer().repaint();
                        this._outlineProvider.repaint();
                    }
                    getPack() {
                        return this._pack;
                    }
                    setInput(file) {
                        super.setInput(file);
                        this.updateContent();
                    }
                    getEditorViewerProvider(key) {
                        switch (key) {
                            case phasereditor2d.outline.ui.views.OutlineView.EDITOR_VIEWER_PROVIDER_KEY:
                                return this._outlineProvider;
                        }
                        return null;
                    }
                }
                editor.AssetPackEditor = AssetPackEditor;
            })(editor = ui.editor || (ui.editor = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                class AssetPackContentProvider {
                    getChildren(parent) {
                        if (parent instanceof pack.core.AssetPack) {
                            return parent.getItems();
                        }
                        if (parent instanceof pack.core.AssetPackItem) {
                            if (parent.getType() === pack.core.IMAGE_TYPE) {
                                return [];
                            }
                            if (pack.core.AssetPackUtils.isImageFrameContainer(parent)) {
                                return pack.core.AssetPackUtils.getImageFrames(parent);
                            }
                        }
                        return [];
                    }
                }
                viewers.AssetPackContentProvider = AssetPackContentProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../viewers/AssetPackContentProvider.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var editor;
            (function (editor_1) {
                class AssetPackEditorContentProvider extends ui.viewers.AssetPackContentProvider {
                    constructor(editor) {
                        super();
                        this._editor = editor;
                    }
                    getPack() {
                        return this._editor.getPack();
                    }
                    getRoots(input) {
                        if (this.getPack()) {
                            return this.getPack().getItems();
                        }
                        return [];
                    }
                    getChildren(parent) {
                        if (typeof (parent) === "string") {
                            const type = parent;
                            if (this.getPack()) {
                                const children = this.getPack().getItems()
                                    .filter(item => item.getType() === type);
                                return children;
                            }
                        }
                        return super.getChildren(parent);
                    }
                }
                editor_1.AssetPackEditorContentProvider = AssetPackEditorContentProvider;
            })(editor = ui.editor || (ui.editor = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var editor;
            (function (editor_2) {
                class AssetPackEditorOutlineContentProvider extends editor_2.AssetPackEditorContentProvider {
                    constructor(editor) {
                        super(editor);
                    }
                    getRoots() {
                        if (this.getPack()) {
                            const types = this.getPack().getItems().map(item => item.getType());
                            const set = new Set(types);
                            const result = pack.core.TYPES.filter(type => set.has(type));
                            return result;
                        }
                        return [];
                    }
                }
                editor_2.AssetPackEditorOutlineContentProvider = AssetPackEditorOutlineContentProvider;
            })(editor = ui.editor || (ui.editor = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var editor;
            (function (editor_3) {
                var ide = colibri.ui.ide;
                var controls = colibri.ui.controls;
                class AssetPackEditorOutlineProvider extends ide.EditorViewerProvider {
                    constructor(editor) {
                        super();
                        this._editor = editor;
                    }
                    getContentProvider() {
                        return new editor_3.AssetPackEditorOutlineContentProvider(this._editor);
                    }
                    getLabelProvider() {
                        return this._editor.getViewer().getLabelProvider();
                    }
                    getCellRendererProvider() {
                        return new ui.viewers.AssetPackCellRendererProvider("tree");
                    }
                    getTreeViewerRenderer(viewer) {
                        return new controls.viewers.TreeViewerRenderer(viewer);
                    }
                    getPropertySectionProvider() {
                        return null;
                    }
                    getInput() {
                        return this._editor.getViewer().getInput();
                    }
                    preload() {
                        return Promise.resolve();
                    }
                }
                editor_3.AssetPackEditorOutlineProvider = AssetPackEditorOutlineProvider;
            })(editor = ui.editor || (ui.editor = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var properties;
            (function (properties) {
                var controls = colibri.ui.controls;
                class AssetPackItemSection extends controls.properties.PropertySection {
                    constructor(page) {
                        super(page, "AssetPackItemPropertySection", "File Key", false);
                    }
                    createForm(parent) {
                        const comp = this.createGridElement(parent, 2);
                        {
                            // Key
                            this.createLabel(comp, "Key");
                            const text = this.createText(comp, true);
                            this.addUpdater(() => {
                                text.value = this.flatValues_StringJoin(this.getSelection().map(item => item.getKey()));
                            });
                        }
                    }
                    canEdit(obj) {
                        return obj instanceof pack.core.AssetPackItem;
                    }
                    canEditNumber(n) {
                        return n === 1;
                    }
                }
                properties.AssetPackItemSection = AssetPackItemSection;
            })(properties = ui.properties || (ui.properties = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var properties;
            (function (properties) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class ImageSection extends controls.properties.PropertySection {
                    constructor(page) {
                        super(page, "pack.ImageSection", "Image", true);
                    }
                    createForm(parent) {
                        parent.classList.add("ImagePreviewFormArea", "PreviewBackground");
                        const imgControl = new controls.ImageControl(ide.IMG_SECTION_PADDING);
                        this.getPage().addEventListener(controls.EVENT_CONTROL_LAYOUT, (e) => {
                            imgControl.resizeTo();
                        });
                        parent.appendChild(imgControl.getElement());
                        setTimeout(() => imgControl.resizeTo(), 1);
                        this.addUpdater(() => {
                            const obj = this.getSelection()[0];
                            let img;
                            if (obj instanceof pack.core.AssetPackItem) {
                                img = pack.core.AssetPackUtils.getImageFromPackUrl(obj.getData().url);
                            }
                            else {
                                img = obj;
                            }
                            imgControl.setImage(img);
                            setTimeout(() => imgControl.resizeTo(), 1);
                        });
                    }
                    canEdit(obj) {
                        return obj instanceof pack.core.AssetPackItem && obj.getType() === "image" || obj instanceof controls.ImageFrame;
                    }
                    canEditNumber(n) {
                        return n === 1;
                    }
                }
                properties.ImageSection = ImageSection;
            })(properties = ui.properties || (ui.properties = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var properties;
            (function (properties) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class ManyImageSection extends controls.properties.PropertySection {
                    constructor(page) {
                        super(page, "phasereditor2d.ui.ide.editors.pack.properties.ManyImageSection", "Images", true);
                    }
                    createForm(parent) {
                        parent.classList.add("ManyImagePreviewFormArea");
                        const viewer = new controls.viewers.TreeViewer("PreviewBackground");
                        viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        viewer.setTreeRenderer(new controls.viewers.GridTreeViewerRenderer(viewer, false, true));
                        viewer.setLabelProvider(new ui.viewers.AssetPackLabelProvider());
                        viewer.setCellRendererProvider(new ui.viewers.AssetPackCellRendererProvider("grid"));
                        const filteredViewer = new ide.properties.FilteredViewerInPropertySection(this.getPage(), viewer);
                        parent.appendChild(filteredViewer.getElement());
                        this.addUpdater(async () => {
                            const frames = await this.getImageFrames();
                            // clean the viewer first
                            viewer.setInput([]);
                            viewer.repaint();
                            viewer.setInput(frames);
                            filteredViewer.resizeTo();
                        });
                    }
                    async getImageFrames() {
                        const frames = this.getSelection().flatMap(obj => {
                            if (obj instanceof pack.core.AssetPackItem) {
                                return pack.core.AssetPackUtils.getImageFrames(obj);
                            }
                            return [obj];
                        });
                        return frames;
                    }
                    canEdit(obj, n) {
                        if (n === 1) {
                            return obj instanceof pack.core.AssetPackItem && obj.getType() !== pack.core.IMAGE_TYPE && pack.core.AssetPackUtils.isImageFrameContainer(obj);
                        }
                        return obj instanceof controls.ImageFrame || obj instanceof pack.core.AssetPackItem && pack.core.AssetPackUtils.isImageFrameContainer(obj);
                    }
                    canEditNumber(n) {
                        return n > 0;
                    }
                }
                properties.ManyImageSection = ManyImageSection;
            })(properties = ui.properties || (ui.properties = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class AssetPackCellRendererProvider {
                    constructor(layout) {
                        this._layout = layout;
                    }
                    getCellRenderer(element) {
                        if (typeof (element) === "string") {
                            return new controls.viewers.IconImageCellRenderer(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER));
                        }
                        else if (element instanceof pack.core.AssetPackItem) {
                            const type = element.getType();
                            const filesPlugin = phasereditor2d.files.FilesPlugin.getInstance();
                            switch (type) {
                                case pack.core.IMAGE_TYPE:
                                    return new viewers.ImageAssetPackItemCellRenderer();
                                case pack.core.MULTI_ATLAS_TYPE:
                                case pack.core.ATLAS_TYPE:
                                case pack.core.UNITY_ATLAS_TYPE:
                                case pack.core.ATLAS_XML_TYPE: {
                                    if (this._layout === "grid") {
                                        return new controls.viewers.FolderCellRenderer();
                                    }
                                    return new viewers.ImageFrameContainerIconCellRenderer();
                                }
                                case pack.core.SPRITESHEET_TYPE:
                                    return new viewers.ImageFrameContainerIconCellRenderer();
                                case pack.core.AUDIO_TYPE:
                                    return this.getIconRenderer(filesPlugin.getIcon(phasereditor2d.files.ICON_FILE_SOUND));
                                case pack.core.SCRIPT_TYPE:
                                case pack.core.SCENE_FILE_TYPE:
                                case pack.core.SCENE_PLUGIN_TYPE:
                                case pack.core.PLUGIN_TYPE:
                                case pack.core.CSS_TYPE:
                                case pack.core.GLSL_TYPE:
                                case pack.core.XML_TYPE:
                                case pack.core.HTML_TYPE:
                                case pack.core.JSON_TYPE:
                                    return this.getIconRenderer(filesPlugin.getIcon(phasereditor2d.files.ICON_FILE_SCRIPT));
                                case pack.core.TEXT_TYPE:
                                    return this.getIconRenderer(filesPlugin.getIcon(phasereditor2d.files.ICON_FILE_TEXT));
                                case pack.core.HTML_TEXTURE_TYPE:
                                    return this.getIconRenderer(filesPlugin.getIcon(phasereditor2d.files.ICON_FILE_IMAGE));
                                case pack.core.BITMAP_FONT_TYPE:
                                    return this.getIconRenderer(filesPlugin.getIcon(phasereditor2d.files.ICON_FILE_FONT));
                                case pack.core.VIDEO_TYPE:
                                    return this.getIconRenderer(filesPlugin.getIcon(phasereditor2d.files.ICON_FILE_VIDEO));
                                default:
                                    break;
                            }
                        }
                        else if (element instanceof controls.ImageFrame) {
                            return new controls.viewers.ImageCellRenderer();
                        }
                        return this.getIconRenderer(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FILE));
                    }
                    getIconRenderer(icon) {
                        if (this._layout === "grid") {
                            return new controls.viewers.IconGridCellRenderer(icon);
                        }
                        return new controls.viewers.IconImageCellRenderer(icon);
                    }
                    preload(element) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                viewers.AssetPackCellRendererProvider = AssetPackCellRendererProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class AssetPackLabelProvider {
                    getLabel(obj) {
                        if (obj instanceof pack.core.AssetPack) {
                            return obj.getFile().getName();
                        }
                        if (obj instanceof pack.core.AssetPackItem) {
                            return obj.getKey();
                        }
                        if (obj instanceof controls.ImageFrame) {
                            return obj.getName();
                        }
                        if (typeof (obj) === "string") {
                            return obj;
                        }
                        return "";
                    }
                }
                viewers.AssetPackLabelProvider = AssetPackLabelProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class AssetPackTreeViewerRenderer extends controls.viewers.GridTreeViewerRenderer {
                    constructor(viewer, flat) {
                        super(viewer, flat, false);
                        viewer.setCellSize(64);
                        const types = pack.core.TYPES.filter(type => type === pack.core.ATLAS_TYPE || type.toLowerCase().indexOf("atlas") < 0);
                        this.setSections(types);
                    }
                    renderCellBack(args, selected, isLastChild) {
                        super.renderCellBack(args, selected, isLastChild);
                        const isParent = this.isParent(args.obj);
                        const isChild = this.isChild(args.obj);
                        const expanded = args.viewer.isExpanded(args.obj);
                        if (isParent && !this.isFlat()) {
                            const ctx = args.canvasContext;
                            ctx.save();
                            ctx.fillStyle = "rgba(0, 0, 0, 0.2)";
                            if (expanded) {
                                controls.Controls.drawRoundedRect(ctx, args.x, args.y, args.w, args.h, 5, 0, 0, 5);
                            }
                            else {
                                controls.Controls.drawRoundedRect(ctx, args.x, args.y, args.w, args.h, 5, 5, 5, 5);
                            }
                            ctx.restore();
                        }
                        else if (isChild) {
                            const margin = controls.viewers.TREE_RENDERER_GRID_PADDING;
                            const ctx = args.canvasContext;
                            ctx.save();
                            ctx.fillStyle = "rgba(0, 0, 0, 0.2)";
                            if (isLastChild) {
                                controls.Controls.drawRoundedRect(ctx, args.x - margin, args.y, args.w + margin, args.h, 0, 5, 5, 0);
                            }
                            else {
                                controls.Controls.drawRoundedRect(ctx, args.x - margin, args.y, args.w + margin, args.h, 0, 0, 0, 0);
                            }
                            ctx.restore();
                        }
                    }
                    isParent(obj) {
                        if (obj instanceof pack.core.AssetPackItem) {
                            switch (obj.getType()) {
                                case pack.core.ATLAS_TYPE:
                                case pack.core.MULTI_ATLAS_TYPE:
                                case pack.core.ATLAS_XML_TYPE:
                                case pack.core.UNITY_ATLAS_TYPE:
                                case pack.core.SPRITESHEET_TYPE:
                                    return true;
                                default:
                                    return false;
                            }
                        }
                        return false;
                    }
                    isChild(obj) {
                        return obj instanceof controls.ImageFrame;
                    }
                }
                viewers.AssetPackTreeViewerRenderer = AssetPackTreeViewerRenderer;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class ImageFrameContainerIconCellRenderer {
                    renderCell(args) {
                        const packItem = args.obj;
                        if (pack.core.AssetPackUtils.isImageFrameContainer(packItem)) {
                            const frames = pack.core.AssetPackUtils.getImageFrames(packItem);
                            if (frames.length > 0) {
                                const img = frames[0].getImage();
                                img.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
                            }
                        }
                    }
                    cellHeight(args) {
                        return args.viewer.getCellSize();
                    }
                    preload(obj) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                viewers.ImageFrameContainerIconCellRenderer = ImageFrameContainerIconCellRenderer;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class ImageAssetPackItemCellRenderer extends controls.viewers.ImageCellRenderer {
                    getImage(obj) {
                        const item = obj;
                        const data = item.getData();
                        return pack.core.AssetPackUtils.getImageFromPackUrl(data.url);
                    }
                }
                viewers.ImageAssetPackItemCellRenderer = ImageAssetPackItemCellRenderer;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
