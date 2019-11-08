namespace phasereditor2d.pack.core.contentTypes {

    import ide = colibri.ui.ide;
    import core = colibri.core;

    export const CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";

    export class AssetPackContentTypeResolver extends core.ContentTypeResolver {

        constructor() {
            super("phasereditor2d.pack.core.AssetPackContentTypeResolver");
        }

        async computeContentType(file: core.io.FilePath): Promise<string> {

            if (file.getExtension() === "json") {

                const content = await ide.FileUtils.preloadAndGetFileString(file);

                if (content !== null) {

                    try {

                        const data = JSON.parse(content);
                        const meta = data["meta"];

                        if (meta["contentType"] === "Phaser v3 Asset Pack") {
                            return CONTENT_TYPE_ASSET_PACK;
                        }

                    } catch (e) {
                        // nothing
                    }
                }
            }

            return core.CONTENT_TYPE_ANY;
        }

    }
}