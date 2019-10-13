/// <reference path="../../../colibri/core/ContentTypeResolver.ts" />

namespace phasereditor2d.files.core {

    import io = colibri.core.io;

    export class ExtensionContentTypeResolver extends colibri.core.ContentTypeResolver {

        private _map: Map<string, string>;

        constructor(id: string, defs: string[][]) {
            super(id);

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