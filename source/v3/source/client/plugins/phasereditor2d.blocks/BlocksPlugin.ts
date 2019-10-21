namespace phasereditor2d.blocks {

    import ide = colibri.ui.ide;

    export const ICON_BLOCKS = "blocks";

    export class BlocksPlugin extends ide.Plugin {

        private static _instance = new BlocksPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.blocks");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_BLOCKS
                ])
            );

        }
    }

    ide.Workbench.getWorkbench().addPlugin(BlocksPlugin.getInstance());
}