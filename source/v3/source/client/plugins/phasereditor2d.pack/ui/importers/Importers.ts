/// <reference path="./MultiatlasImporter.ts" />
/// <reference path="./AtlasXMLImporter.ts" />
/// <reference path="./UnityAtlasImporter.ts" />
/// <reference path="./SingleFileImporter.ts" />

namespace phasereditor2d.pack.ui.importers {

    export class Importers {

        static LIST = [

            new AtlasImporter(),
            new MultiatlasImporter(),
            new AtlasXMLImporter(),
            new UnityAtlasImporter(),
            new SingleFileImporter(files.core.CONTENT_TYPE_IMAGE, core.IMAGE_TYPE)
        ]

        static getImporter(type: string) {
            return this.LIST.find(i => i.getType() === type);
        }
    }
}