/// <reference path="./Importer.ts" />

namespace phasereditor2d.pack.ui.importers {

    import ide = colibri.ui.ide;
    import io = colibri.core.io;

    export class SingleFileImporter extends ContentTypeImporter {

        acceptFile(file: io.FilePath): boolean {

            const fileContentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);

            return fileContentType === this.getContentType();
        }

        createItemData(file: io.FilePath) {

            return {
                url: core.AssetPackUtils.getFilePackUrl(file)
            }
        }
    }

}