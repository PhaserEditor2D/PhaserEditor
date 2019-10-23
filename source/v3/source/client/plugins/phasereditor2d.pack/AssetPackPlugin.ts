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

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.AtlasContentTypeResolver",
                    [new pack.core.AtlasContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.MultiatlasContentTypeResolver",
                    [new pack.core.MultiatlasContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.AtlasXMLContentTypeResolver",
                    [new pack.core.AtlasXMLContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.UnityAtlasContentTypeResolver",
                    [new pack.core.UnityAtlasContentTypeResolver()],
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

            // project resources preloader


            reg.addExtension(ide.PreloadProjectResourcesExtension.POINT_ID,
                new ide.PreloadProjectResourcesExtension(
                    "phasereditor2d.pack.PreloadProjectResourcesExtension",
                    () => pack.core.PackFinder.preload()
                )
            );

            // editors

            reg.addExtension(ide.EditorExtension.POINT_ID,
                new ide.EditorExtension("phasereditor2d.pack.EditorExtension", [
                    ui.editor.AssetPackEditor.getFactory()
                ]));
        }
    }

    ide.Workbench.getWorkbench().addPlugin(AssetPackPlugin.getInstance());
}