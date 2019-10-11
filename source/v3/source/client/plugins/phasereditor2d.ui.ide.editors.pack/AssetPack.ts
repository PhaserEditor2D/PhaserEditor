namespace phasereditor2d.ui.ide.editors.pack {

    export const IMAGE_TYPE = "image";
    export const ATLAS_TYPE = "atlas";
    export const ATLAS_XML_TYPE = "atlasXML";
    export const UNITY_ATLAS_TYPE = "unityAtlas";
    export const MULTI_ATLAS_TYPE = "multiatlas";
    export const SPRITESHEET_TYPE = "spritesheet";

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
            const content = await FileUtils.preloadAndGetFileString(file);
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