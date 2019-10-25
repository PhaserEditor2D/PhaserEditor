var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_ANIMATIONS = "Phaser v3 Animations";
                class AnimationsContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.AnimationsContentTypeResolver";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "json") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const data = JSON.parse(content);
                            if (data.meta) {
                                if (data.meta.contentType === contentTypes.CONTENT_TYPE_ANIMATIONS) {
                                    return contentTypes.CONTENT_TYPE_ANIMATIONS;
                                }
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.AnimationsContentTypeResolver = AnimationsContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./core/contentTypes/AnimationsContentTypeResolver.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ide = colibri.ui.ide;
        pack.ICON_ASSET_PACK = "asset-pack";
        pack.ICON_ANIMATIONS = "animations";
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
                    pack.ICON_ASSET_PACK,
                    pack.ICON_ANIMATIONS
                ]));
                // content type resolvers
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AssetPackContentTypeResolver", [new pack.core.contentTypes.AssetPackContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AtlasContentTypeResolver", [new pack.core.contentTypes.AtlasContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.MultiatlasContentTypeResolver", [new pack.core.contentTypes.MultiatlasContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AtlasXMLContentTypeResolver", [new pack.core.contentTypes.AtlasXMLContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.UnityAtlasContentTypeResolver", [new pack.core.contentTypes.UnityAtlasContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AnimationsContentTypeResolver", [new pack.core.contentTypes.AnimationsContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.BitmapFontContentTypeResolver", [new pack.core.contentTypes.BitmapFontContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.TilemapImpactContentTypeResolver", [new pack.core.contentTypes.TilemapImpactContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.TilemapTiledJSONContentTypeResolver", [new pack.core.contentTypes.TilemapTiledJSONContentTypeResolver()], 5));
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AudioSpriteContentTypeResolver", [new pack.core.contentTypes.AudioSpriteContentTypeResolver()], 5));
                // content type icons
                reg.addExtension(ide.ContentTypeIconExtension.POINT_ID, ide.ContentTypeIconExtension.withPluginIcons(this, [
                    {
                        iconName: pack.ICON_ASSET_PACK,
                        contentType: pack.core.contentTypes.CONTENT_TYPE_ASSET_PACK
                    },
                    {
                        iconName: pack.ICON_ANIMATIONS,
                        contentType: pack.core.contentTypes.CONTENT_TYPE_ANIMATIONS
                    },
                    {
                        iconName: phasereditor2d.files.ICON_FILE_FONT,
                        contentType: pack.core.contentTypes.CONTENT_TYPE_BITMAP_FONT
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
    (function (pack_1) {
        var core;
        (function (core) {
            var controls = colibri.ui.controls;
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
                addToPhaserCache(game) {
                }
                async preload() {
                    return controls.Controls.resolveNothingLoaded();
                }
            }
            core.AssetPackItem = AssetPackItem;
        })(core = pack_1.core || (pack_1.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_2) {
        var core;
        (function (core) {
            class AnimationsAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.AnimationsAssetPackItem = AnimationsAssetPackItem;
        })(core = pack_2.core || (pack_2.core = {}));
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
                                        const item = this.createPackItem(fileData);
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
                toJSON() {
                    return {
                        "section1": {
                            "files": this._items.map(item => item.getData())
                        },
                        "meta": {
                            "app": "Phaser Editor 2D - Asset Pack Editor",
                            "contentType": "Phaser v3 Asset Pack",
                            "url": "https://phasereditor2d.com",
                            "version": "2"
                        }
                    };
                }
                createPackItem(data) {
                    const type = data.type;
                    switch (type) {
                        case core_1.IMAGE_TYPE:
                            return new core_1.ImageAssetPackItem(this, data);
                        case core_1.SVG_TYPE:
                            return new core_1.SvgAssetPackItem(this, data);
                        case core_1.ATLAS_TYPE:
                            return new core_1.AtlasAssetPackItem(this, data);
                        case core_1.ATLAS_XML_TYPE:
                            return new core_1.AtlasXMLAssetPackItem(this, data);
                        case core_1.UNITY_ATLAS_TYPE:
                            return new core_1.UnityAtlasAssetPackItem(this, data);
                        case core_1.MULTI_ATLAS_TYPE:
                            return new core_1.MultiatlasAssetPackItem(this, data);
                        case core_1.SPRITESHEET_TYPE:
                            return new core_1.SpritesheetAssetPackItem(this, data);
                        case core_1.ANIMATIONS_TYPE:
                            return new core_1.AnimationsAssetPackItem(this, data);
                        case core_1.BITMAP_FONT_TYPE:
                            return new core_1.BitmapFontAssetPackItem(this, data);
                        case core_1.TILEMAP_CSV_TYPE:
                            return new core_1.TilemapCSVAssetPackItem(this, data);
                        case core_1.TILEMAP_IMPACT_TYPE:
                            return new core_1.TilemapImpactAssetPackItem(this, data);
                        case core_1.TILEMAP_TILED_JSON_TYPE:
                            return new core_1.TilemapTiledJSONAssetPackItem(this, data);
                        case core_1.PLUGIN_TYPE:
                            return new core_1.PluginAssetPackItem(this, data);
                        case core_1.SCENE_FILE_TYPE:
                            return new core_1.SceneFileAssetPackItem(this, data);
                        case core_1.SCENE_PLUGIN_TYPE:
                            return new core_1.ScenePluginAssetPackItem(this, data);
                        case core_1.SCRIPT_TYPE:
                            return new core_1.ScriptAssetPackItem(this, data);
                        case core_1.AUDIO_TYPE:
                            return new core_1.AudioAssetPackItem(this, data);
                        case core_1.AUDIO_SPRITE_TYPE:
                            return new core_1.AudioSpriteAssetPackItem(this, data);
                        case core_1.VIDEO_TYPE:
                            return new core_1.VideoAssetPackItem(this, data);
                        case core_1.TEXT_TYPE:
                            return new core_1.TextAssetPackItem(this, data);
                        case core_1.CSS_TYPE:
                            return new core_1.CssAssetPackItem(this, data);
                        case core_1.GLSL_TYPE:
                            return new core_1.GlslAssetPackItem(this, data);
                        case core_1.HTML_TYPE:
                            return new core_1.HTMLAssetPackItem(this, data);
                        case core_1.HTML_TEXTURE_TYPE:
                            return new core_1.HTMLTextureAssetPackItem(this, data);
                        case core_1.BINARY_TYPE:
                            return new core_1.BinaryAssetPackItem(this, data);
                        case core_1.JSON_TYPE:
                            return new core_1.JSONAssetPackItem(this, data);
                        case core_1.XML_TYPE:
                            return new core_1.XMLAssetPackItem(this, data);
                    }
                    return new core_1.AssetPackItem(this, data);
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
    (function (pack_3) {
        var core;
        (function (core) {
            var ide = colibri.ui.ide;
            const ATLAS_TYPES = new Set([
                core.MULTI_ATLAS_TYPE,
                core.ATLAS_TYPE,
                core.UNITY_ATLAS_TYPE,
                core.ATLAS_XML_TYPE,
            ]);
            class AssetPackUtils {
                static isAtlasType(type) {
                    return ATLAS_TYPES.has(type);
                }
                static async preloadAssetPackItems(packItems) {
                    for (const item of packItems) {
                        await item.preload();
                    }
                }
                static async getAllPacks() {
                    const files = await ide.FileUtils.getFilesWithContentType(core.contentTypes.CONTENT_TYPE_ASSET_PACK);
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
                static getFilePackUrl(file) {
                    if (file.getParent()) {
                        return `${this.getFilePackUrl(file.getParent())}${file.getName()}${file.isFolder() ? "/" : ""}`;
                    }
                    return "";
                }
                static getFilePackUrlWithNewExtension(file, ext) {
                    const url = this.getFilePackUrl(file.getParent());
                    return `${url}${file.getNameWithoutExtension()}.${ext}`;
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
        })(core = pack_3.core || (pack_3.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_4) {
        var core;
        (function (core) {
            var controls = colibri.ui.controls;
            class ImageFrameContainerAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                    this._frames = null;
                }
                async preload() {
                    if (this._frames) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                    const parser = this.createParser();
                    return parser.preloadFrames();
                }
                findFrame(frameName) {
                    return this.getFrames().find(f => f.getName() === frameName);
                }
                getFrames() {
                    if (this._frames === null) {
                        const parser = this.createParser();
                        this._frames = parser.parseFrames();
                    }
                    return this._frames;
                }
                addToPhaserCache(game) {
                    const parser = this.createParser();
                    parser.addToPhaserCache(game);
                }
            }
            core.ImageFrameContainerAssetPackItem = ImageFrameContainerAssetPackItem;
        })(core = pack_4.core || (pack_4.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./ImageFrameContainerAssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            class BaseAtlasAssetPackItem extends core.ImageFrameContainerAssetPackItem {
            }
            core.BaseAtlasAssetPackItem = BaseAtlasAssetPackItem;
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasAssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_5) {
        var core;
        (function (core) {
            class AtlasAssetPackItem extends core.BaseAtlasAssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
                createParser() {
                    return new core.parsers.AtlasParser(this);
                }
            }
            core.AtlasAssetPackItem = AtlasAssetPackItem;
        })(core = pack_5.core || (pack_5.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_6) {
        var core;
        (function (core) {
            class AtlasXMLAssetPackItem extends core.BaseAtlasAssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
                createParser() {
                    return new core.parsers.AtlasXMLParser(this);
                }
            }
            core.AtlasXMLAssetPackItem = AtlasXMLAssetPackItem;
        })(core = pack_6.core || (pack_6.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_7) {
        var core;
        (function (core) {
            class AudioAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.AudioAssetPackItem = AudioAssetPackItem;
        })(core = pack_7.core || (pack_7.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_8) {
        var core;
        (function (core) {
            class AudioSpriteAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.AudioSpriteAssetPackItem = AudioSpriteAssetPackItem;
        })(core = pack_8.core || (pack_8.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_9) {
        var core;
        (function (core) {
            class BinaryAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.BinaryAssetPackItem = BinaryAssetPackItem;
        })(core = pack_9.core || (pack_9.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_10) {
        var core;
        (function (core) {
            class BitmapFontAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.BitmapFontAssetPackItem = BitmapFontAssetPackItem;
        })(core = pack_10.core || (pack_10.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_11) {
        var core;
        (function (core) {
            class CssAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.CssAssetPackItem = CssAssetPackItem;
        })(core = pack_11.core || (pack_11.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_12) {
        var core;
        (function (core) {
            class GlslAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.GlslAssetPackItem = GlslAssetPackItem;
        })(core = pack_12.core || (pack_12.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_13) {
        var core;
        (function (core) {
            class HTMLAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.HTMLAssetPackItem = HTMLAssetPackItem;
        })(core = pack_13.core || (pack_13.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_14) {
        var core;
        (function (core) {
            class HTMLTextureAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.HTMLTextureAssetPackItem = HTMLTextureAssetPackItem;
        })(core = pack_14.core || (pack_14.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_15) {
        var core;
        (function (core) {
            class ImageAssetPackItem extends core.ImageFrameContainerAssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
                createParser() {
                    return new core.parsers.ImageParser(this);
                }
            }
            core.ImageAssetPackItem = ImageAssetPackItem;
        })(core = pack_15.core || (pack_15.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_16) {
        var core;
        (function (core) {
            class JSONAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.JSONAssetPackItem = JSONAssetPackItem;
        })(core = pack_16.core || (pack_16.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_17) {
        var core;
        (function (core) {
            class MultiatlasAssetPackItem extends core.BaseAtlasAssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
                createParser() {
                    return new core.parsers.MultiAtlasParser(this);
                }
            }
            core.MultiatlasAssetPackItem = MultiatlasAssetPackItem;
        })(core = pack_17.core || (pack_17.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_18) {
        var core;
        (function (core_2) {
            var controls = colibri.ui.controls;
            class PackFinder {
                constructor() {
                }
                static async preload() {
                    if (this._loaded) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                    this._packs = await core_2.AssetPackUtils.getAllPacks();
                    const items = this._packs.flatMap(pack => pack.getItems());
                    await core_2.AssetPackUtils.preloadAssetPackItems(items);
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
                    if (item.getType() === core_2.IMAGE_TYPE) {
                        if (frame === null || frame === undefined) {
                            return item;
                        }
                        return null;
                    }
                    else if (item instanceof core_2.ImageFrameContainerAssetPackItem) {
                        const imageFrame = item.findFrame(frame);
                        return imageFrame;
                    }
                    return item;
                }
                static getAssetPackItemImage(key, frame) {
                    const asset = this.getAssetPackItemOrFrame(key, frame);
                    if (asset instanceof core_2.AssetPackItem && asset.getType() === core_2.IMAGE_TYPE) {
                        return core_2.AssetPackUtils.getImageFromPackUrl(asset.getData().url);
                    }
                    else if (asset instanceof core_2.AssetPackImageFrame) {
                        return asset;
                    }
                    return new controls.ImageWrapper(null);
                }
            }
            PackFinder._packs = [];
            PackFinder._loaded = false;
            core_2.PackFinder = PackFinder;
        })(core = pack_18.core || (pack_18.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_19) {
        var core;
        (function (core) {
            class PluginAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.PluginAssetPackItem = PluginAssetPackItem;
        })(core = pack_19.core || (pack_19.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_20) {
        var core;
        (function (core) {
            class SceneFileAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.SceneFileAssetPackItem = SceneFileAssetPackItem;
        })(core = pack_20.core || (pack_20.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_21) {
        var core;
        (function (core) {
            class ScenePluginAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.ScenePluginAssetPackItem = ScenePluginAssetPackItem;
        })(core = pack_21.core || (pack_21.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_22) {
        var core;
        (function (core) {
            class ScriptAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.ScriptAssetPackItem = ScriptAssetPackItem;
        })(core = pack_22.core || (pack_22.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_23) {
        var core;
        (function (core) {
            class SpritesheetAssetPackItem extends core.ImageFrameContainerAssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
                createParser() {
                    return new core.parsers.SpriteSheetParser(this);
                }
            }
            core.SpritesheetAssetPackItem = SpritesheetAssetPackItem;
        })(core = pack_23.core || (pack_23.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_24) {
        var core;
        (function (core) {
            class SvgAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.SvgAssetPackItem = SvgAssetPackItem;
        })(core = pack_24.core || (pack_24.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_25) {
        var core;
        (function (core) {
            class TextAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.TextAssetPackItem = TextAssetPackItem;
        })(core = pack_25.core || (pack_25.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_26) {
        var core;
        (function (core) {
            class TilemapCSVAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.TilemapCSVAssetPackItem = TilemapCSVAssetPackItem;
        })(core = pack_26.core || (pack_26.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_27) {
        var core;
        (function (core) {
            class TilemapImpactAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.TilemapImpactAssetPackItem = TilemapImpactAssetPackItem;
        })(core = pack_27.core || (pack_27.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_28) {
        var core;
        (function (core) {
            class TilemapTiledJSONAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.TilemapTiledJSONAssetPackItem = TilemapTiledJSONAssetPackItem;
        })(core = pack_28.core || (pack_28.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_29) {
        var core;
        (function (core) {
            class UnityAtlasAssetPackItem extends core.BaseAtlasAssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
                createParser() {
                    return new core.parsers.UnityAtlasParser(this);
                }
            }
            core.UnityAtlasAssetPackItem = UnityAtlasAssetPackItem;
        })(core = pack_29.core || (pack_29.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_30) {
        var core;
        (function (core) {
            class VideoAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.VideoAssetPackItem = VideoAssetPackItem;
        })(core = pack_30.core || (pack_30.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AssetPackItem.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_31) {
        var core;
        (function (core) {
            class XMLAssetPackItem extends core.AssetPackItem {
                constructor(pack, data) {
                    super(pack, data);
                }
            }
            core.XMLAssetPackItem = XMLAssetPackItem;
        })(core = pack_31.core || (pack_31.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core_3) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                var core = colibri.core;
                contentTypes.CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";
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
                                        return contentTypes.CONTENT_TYPE_ASSET_PACK;
                                    }
                                }
                                catch (e) {
                                }
                            }
                        }
                        return core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.AssetPackContentTypeResolver = AssetPackContentTypeResolver;
            })(contentTypes = core_3.contentTypes || (core_3.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_ATLAS = "phasereditor2d.pack.core.atlas";
                class AtlasContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.atlasHashOrArray";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "json") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const data = JSON.parse(content);
                            if (data.hasOwnProperty("frames")) {
                                const frames = data["frames"];
                                if (typeof (frames) === "object") {
                                    return contentTypes.CONTENT_TYPE_ATLAS;
                                }
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.AtlasContentTypeResolver = AtlasContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_ATLAS_XML = "phasereditor2d.pack.core.atlasXML";
                class AtlasXMLContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.atlasXML";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "xml") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const parser = new DOMParser();
                            const xmlDoc = parser.parseFromString(content, "text/xml");
                            const elements = xmlDoc.getElementsByTagName("TextureAtlas");
                            if (elements.length === 1) {
                                return contentTypes.CONTENT_TYPE_ATLAS_XML;
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.AtlasXMLContentTypeResolver = AtlasXMLContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_AUDIO_SPRITE = "phasereditor2d.pack.core.audioSprite";
                class AudioSpriteContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.contentTypes.AudioSpriteContentTypeResolver";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "json") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const data = JSON.parse(content);
                            if (Array.isArray(data.resources) && typeof (data.spritemap) === "object") {
                                return contentTypes.CONTENT_TYPE_AUDIO_SPRITE;
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.AudioSpriteContentTypeResolver = AudioSpriteContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_BITMAP_FONT = "phasereditor2d.pack.core.bitmapFont";
                class BitmapFontContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.BitmapFontContentTypeResolver";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "xml") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const parser = new DOMParser();
                            const xmlDoc = parser.parseFromString(content, "text/xml");
                            const fontElements = xmlDoc.getElementsByTagName("font");
                            const charsElements = xmlDoc.getElementsByTagName("chars");
                            if (fontElements.length === 1 && charsElements.length === 1) {
                                return contentTypes.CONTENT_TYPE_BITMAP_FONT;
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.BitmapFontContentTypeResolver = BitmapFontContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_MULTI_ATLAS = "phasereditor2d.pack.core.multiAtlas";
                class MultiatlasContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.atlasHashOrArray";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "json") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const data = JSON.parse(content);
                            if (data.hasOwnProperty("textures")) {
                                const frames = data["textures"];
                                if (typeof (frames) === "object") {
                                    return contentTypes.CONTENT_TYPE_MULTI_ATLAS;
                                }
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.MultiatlasContentTypeResolver = MultiatlasContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_TILEMAP_IMPACT = "phasereditor2d.pack.core.contentTypes.tilemapImpact";
                class TilemapImpactContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.contentTypes.TilemapImpactContentTypeResolver";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "json") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const data = JSON.parse(content);
                            if (Array.isArray(data.entities) && Array.isArray(data.layer)) {
                                return contentTypes.CONTENT_TYPE_TILEMAP_IMPACT;
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.TilemapImpactContentTypeResolver = TilemapImpactContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                var ide = colibri.ui.ide;
                contentTypes.CONTENT_TYPE_TILEMAP_TILED_JSON = "phasereditor2d.pack.core.contentTypes.tilemapTiledJSON";
                class TilemapTiledJSONContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.contentTypes.TilemapTiledJSONContentTypeResolver";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "json") {
                            const content = await ide.FileUtils.preloadAndGetFileString(file);
                            const data = JSON.parse(content);
                            if (Array.isArray(data.layers)
                                && Array.isArray(data.tilesets)
                                && typeof (data.tilewidth === "number")
                                && typeof (data.tileheight)) {
                                return contentTypes.CONTENT_TYPE_TILEMAP_TILED_JSON;
                            }
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.TilemapTiledJSONContentTypeResolver = TilemapTiledJSONContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
        })(core = pack.core || (pack.core = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var core;
        (function (core) {
            var contentTypes;
            (function (contentTypes) {
                contentTypes.CONTENT_TYPE_UNITY_ATLAS = "phasereditor2d.pack.core.unityAtlas";
                class UnityAtlasContentTypeResolver {
                    getId() {
                        return "phasereditor2d.pack.core.unityAtlas";
                    }
                    async computeContentType(file) {
                        if (file.getExtension() === "meta") {
                            return contentTypes.CONTENT_TYPE_UNITY_ATLAS;
                        }
                        return colibri.core.CONTENT_TYPE_ANY;
                    }
                }
                contentTypes.UnityAtlasContentTypeResolver = UnityAtlasContentTypeResolver;
            })(contentTypes = core.contentTypes || (core.contentTypes = {}));
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
                class ImageFrameParser {
                    constructor(packItem) {
                        this._packItem = packItem;
                    }
                    getPackItem() {
                        return this._packItem;
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
        (function (core) {
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
                            const atlasData = core.AssetPackUtils.getFileJSONFromPackUrl(atlasURL);
                            const textureURL = item.getData().textureURL;
                            const image = core.AssetPackUtils.getImageFromPackUrl(textureURL);
                            if (image) {
                                game.textures.addAtlas(item.getKey(), image.getImageElement(), atlasData);
                            }
                        }
                    }
                    async preloadFrames() {
                        const data = this.getPackItem().getData();
                        const dataFile = core.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                        let result1 = await ide.FileUtils.preloadFileString(dataFile);
                        const imageFile = core.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                        const image = ide.FileUtils.getImage(imageFile);
                        if (image) {
                            let result2 = await image.preload();
                            return Math.max(result1, result2);
                        }
                        return result1;
                    }
                    parseFrames() {
                        const list = [];
                        const data = this.getPackItem().getData();
                        const dataFile = core.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                        const imageFile = core.AssetPackUtils.getFileFromPackUrl(data.textureURL);
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
        (function (core_4) {
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
                            const atlasData = core_4.AssetPackUtils.getFileXMLFromPackUrl(atlasURL);
                            const textureURL = item.getData().textureURL;
                            const image = core_4.AssetPackUtils.getImageFromPackUrl(textureURL);
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
                                imageFrames.push(new core_4.AssetPackImageFrame(this.getPackItem(), name, image, fd));
                            }
                        }
                        catch (e) {
                            console.error(e);
                        }
                    }
                }
                parsers.AtlasXMLParser = AtlasXMLParser;
            })(parsers = core_4.parsers || (core_4.parsers = {}));
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
        (function (core_5) {
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
                            const atlasDataFile = core_5.AssetPackUtils.getFileFromPackUrl(packItemData.url);
                            const atlasData = core_5.AssetPackUtils.getFileJSONFromPackUrl(packItemData.url);
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
                        const dataFile = core_5.AssetPackUtils.getFileFromPackUrl(data.url);
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
                        const dataFile = core_5.AssetPackUtils.getFileFromPackUrl(data.url);
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
            })(parsers = core_5.parsers || (core_5.parsers = {}));
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
                var ide = colibri.ui.ide;
                class SpriteSheetParser extends parsers.ImageFrameParser {
                    constructor(packItem) {
                        super(packItem);
                    }
                    addToPhaserCache(game) {
                        const item = this.getPackItem();
                        if (!game.textures.exists(item.getKey())) {
                            const data = item.getData();
                            const image = core.AssetPackUtils.getImageFromPackUrl(data.url);
                            game.textures.addSpriteSheet(item.getKey(), image.getImageElement(), data.frameConfig);
                        }
                    }
                    async preloadFrames() {
                        const data = this.getPackItem().getData();
                        const imageFile = core.AssetPackUtils.getFileFromPackUrl(data.url);
                        const image = ide.FileUtils.getImage(imageFile);
                        return await image.preload();
                    }
                    parseFrames() {
                        const frames = [];
                        const data = this.getPackItem().getData();
                        const imageFile = core.AssetPackUtils.getFileFromPackUrl(data.url);
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
                                    const fd = new controls.FrameData(i, new controls.Rect(x, y, w, h), new controls.Rect(0, 0, w, h), new controls.Point(w, h));
                                    frames.push(new core.AssetPackImageFrame(this.getPackItem(), i.toString(), image, fd));
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
            })(parsers = core.parsers || (core.parsers = {}));
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
                        if (!image) {
                            return;
                        }
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
                var dialogs = controls.dialogs;
                var io = colibri.core.io;
                class AssetPackEditorFactory extends ide.EditorFactory {
                    constructor() {
                        super("phasereditor2d.AssetPackEditorFactory");
                    }
                    acceptInput(input) {
                        if (input instanceof io.FilePath) {
                            const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                            return contentType === pack.core.contentTypes.CONTENT_TYPE_ASSET_PACK;
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
                        this._propertySectionProvider = new editor.AssetPackEditorPropertySectionProvider();
                        this.addClass("AssetPackEditor");
                    }
                    static getFactory() {
                        return new AssetPackEditorFactory();
                    }
                    createViewer() {
                        const viewer = new controls.viewers.TreeViewer();
                        viewer.setContentProvider(new editor.AssetPackEditorContentProvider(this, true));
                        viewer.setLabelProvider(new ui.viewers.AssetPackLabelProvider());
                        viewer.setCellRendererProvider(new ui.viewers.AssetPackCellRendererProvider("grid"));
                        viewer.setTreeRenderer(new ui.viewers.AssetPackTreeViewerRenderer(viewer, true));
                        viewer.setInput(this);
                        viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                            this._outlineProvider.setSelection(viewer.getSelection(), true, true);
                            this._outlineProvider.repaint();
                        });
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
                    async save() {
                        const content = JSON.stringify(this._pack.toJSON(), null, 4);
                        try {
                            await ide.FileUtils.setFileString_async(this.getInput(), content);
                            this.setDirty(false);
                        }
                        catch (e) {
                            console.error(e);
                        }
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
                    getPropertyProvider() {
                        return this._propertySectionProvider;
                    }
                    createEditorToolbar(parent) {
                        const manager = new controls.ToolbarManager(parent);
                        manager.add(new controls.Action({
                            text: "Add File",
                            icon: ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_PLUS),
                            callback: () => {
                                this.openAddFileDialog();
                            }
                        }));
                        return manager;
                    }
                    openAddFileDialog() {
                        const viewer = new controls.viewers.TreeViewer();
                        viewer.setLabelProvider(new ui.viewers.AssetPackLabelProvider());
                        viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        viewer.setCellRendererProvider(new ui.viewers.AssetPackCellRendererProvider("tree"));
                        viewer.setInput(pack.core.TYPES);
                        const dlg = new dialogs.ViewerDialog(viewer);
                        dlg.create();
                        viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, e => {
                            const type = viewer.getSelection()[0];
                            this.openSelectFileDialog(dlg, type);
                        });
                    }
                    openSelectFileDialog(prevDialog, type) {
                        const viewer = new controls.viewers.TreeViewer();
                        viewer.setLabelProvider(new phasereditor2d.files.ui.viewers.FileLabelProvider());
                        viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        viewer.setCellRendererProvider(new phasereditor2d.files.ui.viewers.FileCellRendererProvider());
                        const folder = this.getInput().getParent();
                        const importer = ui.importers.Importers.getImporter(type);
                        const allFiles = folder.flatTree([], false);
                        const list = allFiles
                            .filter(file => importer.acceptFile(file));
                        viewer.setInput(list);
                        const dlg = new dialogs.ViewerDialog(viewer);
                        dlg.create();
                        const importFiles = async (files) => {
                            dlg.close();
                            prevDialog.close();
                            if (files.length === 0) {
                                //TODO: importer.createEmptyItem(this._pack, file)
                            }
                            else {
                                for (const file of files) {
                                    const item = await importer.importFile(this._pack, file);
                                    await item.preload();
                                }
                            }
                            this._viewer.repaint();
                            this.setDirty(true);
                        };
                        dlg.addAcceptButton("Select", () => {
                            importFiles(viewer.getSelection());
                        });
                        viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, async (e) => {
                            importFiles([viewer.getSelection()[0]]);
                        });
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
                        if (parent instanceof pack.core.ImageAssetPackItem) {
                            return [];
                        }
                        if (parent instanceof pack.core.ImageFrameContainerAssetPackItem) {
                            return parent.getFrames();
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
                    constructor(editor, groupAtlasItems) {
                        super();
                        this._editor = editor;
                        this._groupAtlasItems = groupAtlasItems;
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
                                    .filter(item => {
                                    if (this._groupAtlasItems) {
                                        if (pack.core.AssetPackUtils.isAtlasType(type) && pack.core.AssetPackUtils.isAtlasType(item.getType())) {
                                            return true;
                                        }
                                    }
                                    return item.getType() === type;
                                });
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
                        super(editor, false);
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
                        return this._editor.getPropertyProvider();
                    }
                    getInput() {
                        return this._editor.getViewer().getInput();
                    }
                    preload() {
                        return Promise.resolve();
                    }
                    onViewerSelectionChanged(selection) {
                        this._editor.getViewer().setSelection(selection, false);
                        this._editor.getViewer().reveal(selection);
                        this._editor.getViewer().repaint();
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
            var editor;
            (function (editor) {
                var controls = colibri.ui.controls;
                class AssetPackEditorPropertySectionProvider extends controls.properties.PropertySectionProvider {
                    addSections(page, sections) {
                        sections.push(new ui.properties.ImageSection(page));
                        sections.push(new ui.properties.ManyImageSection(page));
                    }
                }
                editor.AssetPackEditorPropertySectionProvider = AssetPackEditorPropertySectionProvider;
            })(editor = ui.editor || (ui.editor = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack_32) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                var ide = colibri.ui.ide;
                class Importer {
                    constructor(type) {
                        this._type = type;
                    }
                    getType() {
                        return this._type;
                    }
                    async importFile(pack, file) {
                        const computer = new ide.utils.NameMaker(item => item.getKey());
                        computer.update(pack.getItems());
                        const data = this.createItemData(file);
                        data.type = this.getType();
                        data.key = computer.makeName(file.getNameWithoutExtension());
                        console.log(data);
                        const item = pack.createPackItem(data);
                        pack.getItems().push(item);
                        await item.preload();
                        return item;
                    }
                }
                importers.Importer = Importer;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack_32.ui || (pack_32.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Importer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                var ide = colibri.ui.ide;
                class ContentTypeImporter extends importers.Importer {
                    constructor(contentType, assetPackItemType) {
                        super(assetPackItemType);
                        this._contentType = contentType;
                    }
                    getContentType() {
                        return this._contentType;
                    }
                    acceptFile(file) {
                        const fileContentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                        return fileContentType === this._contentType;
                    }
                }
                importers.ContentTypeImporter = ContentTypeImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./ContentTypeImporter.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                var ide = colibri.ui.ide;
                class BaseAtlasImporter extends importers.ContentTypeImporter {
                    acceptFile(file) {
                        const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                        return contentType === this.getContentType();
                    }
                    createItemData(file) {
                        let textureURL;
                        if (file.getNameWithoutExtension().endsWith(".png")) {
                            textureURL = pack.core.AssetPackUtils.getFilePackUrl(file.getParent()) + file.getNameWithoutExtension();
                        }
                        else {
                            textureURL = pack.core.AssetPackUtils.getFilePackUrlWithNewExtension(file, "png");
                        }
                        const altTextureFile = file.getParent().getFile(file.getName() + ".png");
                        if (altTextureFile) {
                            textureURL = pack.core.AssetPackUtils.getFilePackUrl(altTextureFile);
                        }
                        return {
                            atlasURL: pack.core.AssetPackUtils.getFilePackUrl(file),
                            textureURL: textureURL
                        };
                    }
                }
                importers.BaseAtlasImporter = BaseAtlasImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasImporter.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class AtlasImporter extends importers.BaseAtlasImporter {
                    constructor() {
                        super(pack.core.contentTypes.CONTENT_TYPE_ATLAS, pack.core.ATLAS_TYPE);
                    }
                }
                importers.AtlasImporter = AtlasImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasImporter.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class AtlasXMLImporter extends importers.BaseAtlasImporter {
                    constructor() {
                        super(pack.core.contentTypes.CONTENT_TYPE_ATLAS_XML, pack.core.ATLAS_XML_TYPE);
                    }
                }
                importers.AtlasXMLImporter = AtlasXMLImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                var ide = colibri.ui.ide;
                class AudioSpriteImporter extends importers.ContentTypeImporter {
                    constructor() {
                        super(pack.core.contentTypes.CONTENT_TYPE_AUDIO_SPRITE, pack.core.AUDIO_SPRITE_TYPE);
                    }
                    createItemData(file) {
                        const reg = ide.Workbench.getWorkbench().getContentTypeRegistry();
                        const baseName = file.getNameWithoutExtension();
                        const urls = file.getParent().getFiles()
                            .filter(f => reg.getCachedContentType(f) === phasereditor2d.files.core.CONTENT_TYPE_AUDIO)
                            .filter(f => f.getNameWithoutExtension() === baseName)
                            .map(f => pack.core.AssetPackUtils.getFilePackUrl(f));
                        return {
                            jsonURL: pack.core.AssetPackUtils.getFilePackUrl(file),
                            audioURL: urls
                        };
                    }
                }
                importers.AudioSpriteImporter = AudioSpriteImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class BitmapFontImporter extends importers.ContentTypeImporter {
                    constructor() {
                        super(pack.core.contentTypes.CONTENT_TYPE_BITMAP_FONT, pack.core.BITMAP_FONT_TYPE);
                    }
                    createItemData(file) {
                        return {
                            textureURL: pack.core.AssetPackUtils.getFilePackUrlWithNewExtension(file, "png"),
                            fontDataURL: pack.core.AssetPackUtils.getFilePackUrl(file)
                        };
                    }
                }
                importers.BitmapFontImporter = BitmapFontImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Importer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class MultiatlasImporter extends importers.ContentTypeImporter {
                    constructor() {
                        super(pack.core.contentTypes.CONTENT_TYPE_MULTI_ATLAS, pack.core.MULTI_ATLAS_TYPE);
                    }
                    createItemData(file) {
                        return {
                            type: pack.core.MULTI_ATLAS_TYPE,
                            url: pack.core.AssetPackUtils.getFilePackUrl(file)
                        };
                    }
                }
                importers.MultiatlasImporter = MultiatlasImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasImporter.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class UnityAtlasImporter extends importers.BaseAtlasImporter {
                    constructor() {
                        super(pack.core.contentTypes.CONTENT_TYPE_UNITY_ATLAS, pack.core.UNITY_ATLAS_TYPE);
                    }
                }
                importers.UnityAtlasImporter = UnityAtlasImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Importer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                var ide = colibri.ui.ide;
                class SingleFileImporter extends importers.ContentTypeImporter {
                    constructor(contentType, assetPackType, urlIsArray = false, defaultValues = {}) {
                        super(contentType, assetPackType);
                        this._urlIsArray = urlIsArray;
                        this._defaultValues = defaultValues;
                    }
                    acceptFile(file) {
                        const fileContentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                        return fileContentType === this.getContentType();
                    }
                    createItemData(file) {
                        const url = pack.core.AssetPackUtils.getFilePackUrl(file);
                        const data = {
                            url: this._urlIsArray ? [url] : url
                        };
                        for (const k in this._defaultValues) {
                            data[k] = this._defaultValues[k];
                        }
                        return data;
                    }
                }
                importers.SingleFileImporter = SingleFileImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class SpritesheetImporter extends importers.SingleFileImporter {
                    constructor() {
                        super(phasereditor2d.files.core.CONTENT_TYPE_IMAGE, pack.core.SPRITESHEET_TYPE);
                    }
                    createItemData(file) {
                        const data = super.createItemData(file);
                        data.frameConfig = {
                            frameWidth: 32,
                            frameHeight: 32
                        };
                        return data;
                    }
                }
                importers.SpritesheetImporter = SpritesheetImporter;
            })(importers = ui.importers || (ui.importers = {}));
        })(ui = pack.ui || (pack.ui = {}));
    })(pack = phasereditor2d.pack || (phasereditor2d.pack = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./MultiatlasImporter.ts" />
/// <reference path="./AtlasXMLImporter.ts" />
/// <reference path="./UnityAtlasImporter.ts" />
/// <reference path="./SingleFileImporter.ts" />
/// <reference path="./SpritesheetImporter.ts" />
/// <reference path="./BitmapFontImporter.ts" />
/// <reference path="../../core/contentTypes/TilemapImpactContentTypeResolver.ts" />
/// <reference path="../../core/contentTypes/TilemapTiledJSONContentTypeResolver.ts" />
/// <reference path="./AudioSpriteImporter.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var pack;
    (function (pack) {
        var ui;
        (function (ui) {
            var importers;
            (function (importers) {
                class Importers {
                    static getImporter(type) {
                        return this.LIST.find(i => i.getType() === type);
                    }
                }
                Importers.LIST = [
                    new importers.AtlasImporter(),
                    new importers.MultiatlasImporter(),
                    new importers.AtlasXMLImporter(),
                    new importers.UnityAtlasImporter(),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_IMAGE, pack.core.IMAGE_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_SVG, pack.core.SVG_TYPE),
                    new importers.SpritesheetImporter(),
                    new importers.SingleFileImporter(pack.core.contentTypes.CONTENT_TYPE_ANIMATIONS, pack.core.ANIMATIONS_TYPE),
                    new importers.BitmapFontImporter(),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_CSV, pack.core.TILEMAP_CSV_TYPE),
                    new importers.SingleFileImporter(pack.core.contentTypes.CONTENT_TYPE_TILEMAP_IMPACT, pack.core.TILEMAP_IMPACT_TYPE),
                    new importers.SingleFileImporter(pack.core.contentTypes.CONTENT_TYPE_TILEMAP_TILED_JSON, pack.core.TILEMAP_TILED_JSON_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_JAVASCRIPT, pack.core.PLUGIN_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_JAVASCRIPT, pack.core.SCENE_FILE_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_JAVASCRIPT, pack.core.SCENE_PLUGIN_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_JAVASCRIPT, pack.core.SCRIPT_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_AUDIO, pack.core.AUDIO_TYPE, true),
                    new importers.AudioSpriteImporter(),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_VIDEO, pack.core.VIDEO_TYPE, true),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_TEXT, pack.core.TEXT_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_CSS, pack.core.CSS_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_HTML, pack.core.HTML_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_HTML, pack.core.HTML_TEXTURE_TYPE, false, {
                        width: 512,
                        height: 512
                    }),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_GLSL, pack.core.GLSL_TYPE),
                    new importers.SingleFileImporter(colibri.core.CONTENT_TYPE_ANY, pack.core.BINARY_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_JSON, pack.core.JSON_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_XML, pack.core.XML_TYPE),
                    new importers.SingleFileImporter(phasereditor2d.files.core.CONTENT_TYPE_GLSL, pack.core.GLSL_TYPE),
                ];
                importers.Importers = Importers;
            })(importers = ui.importers || (ui.importers = {}));
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
                            if (obj instanceof pack.core.ImageFrameContainerAssetPackItem) {
                                return obj.getFrames();
                            }
                            return [obj];
                        });
                        return frames;
                    }
                    canEdit(obj, n) {
                        if (n === 1) {
                            return obj instanceof pack.core.AssetPackItem && obj.getType() !== pack.core.IMAGE_TYPE && obj instanceof pack.core.ImageFrameContainerAssetPackItem;
                        }
                        return obj instanceof controls.ImageFrame || obj instanceof pack.core.AssetPackItem && obj instanceof pack.core.ImageFrameContainerAssetPackItem;
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
                        if (packItem instanceof pack.core.ImageFrameContainerAssetPackItem) {
                            const frames = packItem.getFrames();
                            if (frames.length > 0) {
                                const img = frames[0].getImage();
                                if (img) {
                                    img.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
                                }
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
