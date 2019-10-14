namespace phasereditor2d.pack {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export const ICON_ASSET_PACK = "asset-pack";

    export class AssetPackPlugin extends ide.Plugin {

        private static _instance = new AssetPackPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.pack");
        }

        async preloadProjectResources() {
            await pack.core.PackFinder.preload();
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // icons loader

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_ASSET_PACK
                ])
            );

            // content type resolvers

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.AssetPackContentTypeResolver",
                    [new pack.core.AssetPackContentTypeResolver()],
                    5
                ));

            // content type icons

            reg.addExtension(
                ide.ContentTypeIconExtension.POINT_ID,
                ide.ContentTypeIconExtension.withPluginIcons(this, [
                    {
                        iconName: ICON_ASSET_PACK,
                        contentType: core.CONTENT_TYPE_ASSET_PACK
                    }
                ]));
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editor.AssetPackEditor.getFactory());
        }

    }

}