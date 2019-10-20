namespace phasereditor2d.pack.core {

    import ide = colibri.ui.ide;
    import core = colibri.core;

    export const IMAGE_TYPE = "image";
    export const ATLAS_TYPE = "atlas";
    export const ATLAS_XML_TYPE = "atlasXML";
    export const UNITY_ATLAS_TYPE = "unityAtlas";
    export const MULTI_ATLAS_TYPE = "multiatlas";
    export const SPRITESHEET_TYPE = "spritesheet";
    export const ANIMATIONS_TYPE = "animations";
    export const AUDIO_TYPE = "audio";
    export const AUDIO_SPRITE_TYPE = "audioSprite";
    export const BINARY_TYPE = "binary";
    export const BITMAP_FONT_TYPE = "bitmapFont";
    export const CSS_TYPE = "css";
    export const GLSL_TYPE = "glsl";
    export const HTML_TYPE = "html";
    export const HTML_TEXTURE_TYPE = "htmlTexture";
    export const JSON_TYPE = "json";
    export const PLUGIN_TYPE = "plugin";
    export const SCENE_FILE_TYPE = "sceneFile";
    export const SCENE_PLUGIN_TYPE = "scenePlugin";
    export const SCRIPT_TYPE = "script";
    export const SVG_TYPE = "svg";
    export const TEXT_TYPE = "text";
    export const TILEMAP_CSV_TYPE = "tilemapCSV";
    export const TILEMAP_IMPACT_TYPE = "tilemapImpact";
    export const TILEMAP_TILED_JSON_TYPE = "tilemapTiledJSON";
    export const VIDEO_TYPE = "video";
    export const XML_TYPE = "xml";

    export const TYPES = [
        IMAGE_TYPE,
        SVG_TYPE,
        ATLAS_TYPE,
        ATLAS_XML_TYPE,
        UNITY_ATLAS_TYPE,
        MULTI_ATLAS_TYPE,
        SPRITESHEET_TYPE,
        ANIMATIONS_TYPE,
        BITMAP_FONT_TYPE,
        TILEMAP_CSV_TYPE,
        TILEMAP_IMPACT_TYPE,
        TILEMAP_TILED_JSON_TYPE,
        PLUGIN_TYPE,
        SCENE_FILE_TYPE,
        SCENE_PLUGIN_TYPE,
        SCRIPT_TYPE,
        AUDIO_TYPE,
        AUDIO_SPRITE_TYPE,
        VIDEO_TYPE,
        TEXT_TYPE,
        CSS_TYPE,
        GLSL_TYPE,
        HTML_TYPE,
        HTML_TEXTURE_TYPE,
        BINARY_TYPE,
        JSON_TYPE,
        XML_TYPE
    ];

    export class AssetPack {
        private _file: core.io.FilePath;
        private _items: AssetPackItem[];

        constructor(file: core.io.FilePath, content: string) {
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
                                const item = new AssetPackItem(this, fileData);
                                this._items.push(item);
                            }
                        }
                    }
                } catch (e) {
                    console.error(e);
                    alert(e.message);
                }
            }
        }

        static async createFromFile(file: core.io.FilePath) {
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
}