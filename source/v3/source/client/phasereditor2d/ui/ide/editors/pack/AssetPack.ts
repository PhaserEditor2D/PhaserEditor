namespace phasereditor2d.ui.ide.editors.pack {

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
            const content = await ui.ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
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