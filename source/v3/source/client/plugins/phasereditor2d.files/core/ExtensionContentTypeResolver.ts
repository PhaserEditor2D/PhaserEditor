namespace phasereditor2d.files.core {

    import io = colibri.core.io;

    export class ExtensionContentTypeResolver implements colibri.core.IContentTypeResolver {

        private _map: Map<string, string>;

        constructor(defs: string[][]) {

            this._map = new Map();

            for (const def of defs) {
                this._map.set(def[0].toUpperCase(), def[1]);
            }
        }

        computeContentType(file: io.FilePath): Promise<string> {

            const ext = file.getExtension().toUpperCase();

            if (this._map.has(ext)) {
                return Promise.resolve(this._map.get(ext));
            }

            return Promise.resolve(colibri.core.CONTENT_TYPE_ANY);
        }

    }

}