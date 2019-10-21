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
            
            list.sort((a, b) => a.getPriority() - b.getPriority());
        }

        getExtensions<T extends Extension>(point: string): T[] {

            let list = this._map.get(point);

            if (!list) {
                return [];
            }

            return <any>list;
        }

    }
}