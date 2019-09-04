namespace phasereditor2d.core {

    export class ContentTypeRegistry {

        private _resolvers: IContentTypeResolver[];
        private _cache: Map<string, string>;

        constructor() {
            this._resolvers = [];
            this._cache = new Map();
        }

        registerResolver(resolver: IContentTypeResolver) {
            this._resolvers.push(resolver);
        }

        getCachedContentType(file: io.FilePath) {
            const id = file.getId();
            
            if (this._cache.has(id)) {
                return this._cache.get(id);
            }

            return CONTENT_TYPE_ANY;
        }

        async preload(file: io.FilePath): Promise<ui.controls.PreloadResult> {
            
            const id = file.getId();

            if (this._cache.has(id)) {
                return ui.controls.Controls.resolveNothingLoaded();
            }

            for (const resolver of this._resolvers) {

                const ct = await resolver.computeContentType(file);

                if (ct !== CONTENT_TYPE_ANY) {
                    this._cache.set(id, ct);
                    return ui.controls.Controls.resolveResourceLoaded();
                }
            }

            return ui.controls.Controls.resolveNothingLoaded();
        }
    }
}