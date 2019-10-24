/// <reference path="./MultiatlasImporter.ts" />
/// <reference path="./AtlasXMLImporter.ts" />
/// <reference path="./UnityAtlasImporter.ts" />
/// <reference path="./SingleFileImporter.ts" />
/// <reference path="./SpritesheetImporter.ts" />
/// <reference path="./BitmapFontImporter.ts" />
/// <reference path="../../core/contentTypes/TilemapImpactContentTypeResolver.ts" />
/// <reference path="../../core/contentTypes/TilemapTiledJSONContentTypeResolver.ts" />
/// <reference path="./AudioSpriteImporter.ts" />

namespace phasereditor2d.pack.ui.importers {

    export class Importers {

        static LIST = [

            new AtlasImporter(),
            new MultiatlasImporter(),
            new AtlasXMLImporter(),
            new UnityAtlasImporter(),
            new SingleFileImporter(files.core.CONTENT_TYPE_IMAGE, core.IMAGE_TYPE),
            new SingleFileImporter(files.core.CONTENT_TYPE_SVG, core.SVG_TYPE),
            new SpritesheetImporter(),
            new SingleFileImporter(core.contentTypes.CONTENT_TYPE_ANIMATIONS, core.ANIMATIONS_TYPE),
            new BitmapFontImporter(),
            new SingleFileImporter(files.core.CONTENT_TYPE_CSV, core.TILEMAP_CSV_TYPE),
            new SingleFileImporter(core.contentTypes.CONTENT_TYPE_TILEMAP_IMPACT, core.TILEMAP_IMPACT_TYPE),
            new SingleFileImporter(core.contentTypes.CONTENT_TYPE_TILEMAP_TILED_JSON, core.TILEMAP_TILED_JSON_TYPE),
            new SingleFileImporter(files.core.CONTENT_TYPE_JAVASCRIPT, core.PLUGIN_TYPE),
            new SingleFileImporter(files.core.CONTENT_TYPE_JAVASCRIPT, core.SCENE_FILE_TYPE),
            new SingleFileImporter(files.core.CONTENT_TYPE_JAVASCRIPT, core.SCENE_PLUGIN_TYPE),
            new SingleFileImporter(files.core.CONTENT_TYPE_JAVASCRIPT, core.SCRIPT_TYPE),
            new SingleFileImporter(files.core.CONTENT_TYPE_AUDIO, core.AUDIO_TYPE, true),
            new AudioSpriteImporter()
        ]

        static getImporter(type: string) {
            return this.LIST.find(i => i.getType() === type);
        }
    }
}