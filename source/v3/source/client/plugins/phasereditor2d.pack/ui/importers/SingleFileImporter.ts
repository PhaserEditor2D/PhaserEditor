/// <reference path="./Importer.ts" />

namespace phasereditor2d.pack.ui.importers {

    import ide = colibri.ui.ide;
    import io = colibri.core.io;

    export class SingleFileImporter extends ContentTypeImporter {
        private _urlIsArray: boolean;

        constructor(contentType: string, assetPackType: string, urlIsArray: boolean = false) {
            super(contentType, assetPackType);

            this._urlIsArray = urlIsArray;
        }

        acceptFile(file: io.FilePath): boolean {

            const fileContentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);

            return fileContentType === this.getContentType();
        }

        createItemData(file: io.FilePath): any {

            const url = core.AssetPackUtils.getFilePackUrl(file);

            return {
                url: this._urlIsArray ? [url] : url
            }
        }
    }

}