/// <reference path="./MultiatlasImporter.ts" />
/// <reference path="./AtlasXMLImporter.ts" />
/// <reference path="./UnityAtlasImporter.ts" />

namespace phasereditor2d.pack.ui.importers {

    export class Importers {

        static LIST = [

            new AtlasImporter(),
            new MultiatlasImporter(),
            new AtlasXMLImporter(),
            new UnityAtlasImporter()
        ]

        static getImporter(type: string) {
            return this.LIST.find(i => i.getType() === type);
        }
    }
}