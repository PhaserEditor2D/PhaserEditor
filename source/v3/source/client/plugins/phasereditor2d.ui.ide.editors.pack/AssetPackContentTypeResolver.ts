namespace phasereditor2d.ui.ide.editors.pack {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export const CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";

    export class AssetPackContentTypeResolver implements core.IContentTypeResolver {

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
                    }
                }
            }

            return core.CONTENT_TYPE_ANY;
        }

    }
}