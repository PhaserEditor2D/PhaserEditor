/// <reference path="./io/FileContentCache.ts" />

namespace phasereditor2d.core {

    export class ContentTypeRegistry {

        private _resolvers: IContentTypeResolver[];
        private _cache: ContentTypeFileCache;

        constructor() {
            this._resolvers = [];
            this._cache = new ContentTypeFileCache(this);
        }

        registerResolver(resolver: IContentTypeResolver) {
            this._resolvers.push(resolver);
        }

        getResolvers() {
            return this._resolvers;
        }

        getCachedContentType(file: io.FilePath) {
            return this._cache.getContent(file);
        }

        async preload(file: io.FilePath): Promise<ui.controls.PreloadResult> {
            return this._cache.preload(file);
        }
    }

    class ContentTypeFileCache extends io.FileContentCache<string> {
        constructor(registry: ContentTypeRegistry) {
            super(async (file) => {

                for (const resolver of registry.getResolvers()) {

                    const ct = await resolver.computeContentType(file);

                    if (ct !== CONTENT_TYPE_ANY) {
                        return ct;
                    }
                }

                return CONTENT_TYPE_ANY;
            });
        }
    }
}