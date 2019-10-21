/// <reference path="./extensions/Extension.ts" />

namespace colibri.core {

    export class ContentTypeExtension extends extensions.Extension {

        static POINT_ID = "colibri.ContentTypeExtension";

        private _resolvers: core.IContentTypeResolver[];

        constructor(id: string, resolvers: core.IContentTypeResolver[], priority: number = 10) {
            super(id, priority);

            this._resolvers = resolvers;
        }

        getResolvers() {
            return this._resolvers;
        }

    }

}