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

        async findContentType(file: io.FilePath): Promise<string> {
            
            const id = file.getId();

            if (this._cache.has(id)) {
                return Promise.resolve(this._cache.get(id));
            }

            for (const resolver of this._resolvers) {

                const ct = await resolver.computeContentType(file);

                if (ct !== CONTENT_TYPE_ANY) {
                    this._cache.set(id, ct);
                    return Promise.resolve(ct);
                }
            }

            return Promise.resolve(CONTENT_TYPE_ANY);
        }
    }
}