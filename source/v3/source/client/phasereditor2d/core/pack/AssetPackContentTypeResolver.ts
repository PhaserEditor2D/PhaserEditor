namespace phasereditor2d.core.pack {

    export const CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";

    export class AssetPackContentTypeResolver implements core.IContentTypeResolver {

        async computeContentType(file: io.FilePath): Promise<string> {
            if (file.getExtension() === "json") {
                const content = await ui.ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
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

            return CONTENT_TYPE_ANY;
        }

    }
}