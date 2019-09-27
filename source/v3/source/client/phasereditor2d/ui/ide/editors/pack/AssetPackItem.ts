namespace phasereditor2d.ui.ide.editors.pack {

    export class AssetPackItem {
        private _pack : AssetPack;
        private _data : any;

        constructor(pack : AssetPack, data : any) {
            this._pack = pack;
            this._data = data;
        }

        getPack() {
            return this._pack;
        }

        getKey() : string {
            return this._data["key"];
        }

        getType() : string {
            return this._data["type"];
        }

        getData() {
            return this._data;
        }
    }

}