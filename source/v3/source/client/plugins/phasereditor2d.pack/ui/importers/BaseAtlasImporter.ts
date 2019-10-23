/// <reference path="./ContentTypeImporter.ts" />

namespace phasereditor2d.pack.ui.importers {

    import io = colibri.core.io;
    import ide = colibri.ui.ide;

    export class BaseAtlasImporter extends ContentTypeImporter {

        acceptFile(file: io.FilePath): boolean {

            const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);

            return contentType === this.getContentType();
        }

        createItemData(file: io.FilePath) {
            return {
                atlasURL: core.AssetPackUtils.getFilePackUrl(file),
                textureURL: core.AssetPackUtils.getFilePackUrlWithNewExtension(file, "png")
            }
        }
    }
}