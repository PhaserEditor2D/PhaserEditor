namespace colibri.core.extensions {

    export class ExtensionRegistry {

        private _map: Map<string, Extension[]>;

        constructor() {
            this._map = new Map();
        }

        addExtension(point: string, ...extension: Extension[]) {

            let list = this._map.get(point);

            if (!list) {
                this._map.set(point, list = []);
            }

            list.push(...extension);
        }

        getExtensions<T extends Extension>(point: string, sorted = false): T[] {

            let list = this._map.get(point);

            if (list) {

                if (sorted) {

                    list = list.slice();
                    list = list.sort((a, b) => a.getPriority() - b.getPriority());
                }

            } else {
                list = [];
            }

            return <any>list;
        }

    }
}