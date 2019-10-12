namespace phasereditor2d.blocks {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_BLOCKS = "blocks";

    export class BlocksPlugin extends ide.Plugin {

        private static _instance = new BlocksPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor.blocks.BlocksPlugin");
        }

        async preloadIcons(contentTypeIconMap: Map<string, controls.IImage>) {
            await this.getIcon(ICON_BLOCKS).preload();
        }

        getIcon(name: string) {
            return controls.Controls.getIcon(name, "plugins/phasereditor2d.blocks/ui/icons");
        }

    }

}