namespace phasereditor2d.outline {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_OUTLINE = "outline";

    export class OutlinePlugin extends ide.Plugin {

        private static _instance = new OutlinePlugin();

        static getInstance() {
            return this._instance;
        }

        constructor() {
            super("phasereditor2d.outline.OutlinePlugin");
        }


        async preloadIcons() {

            await this.getIcon(ICON_OUTLINE).preload();

        }

        getIcon(name: string) {
            return controls.Controls.getIcon(name, "plugins/phasereditor2d.outline/ui/icons");
        }

    }

}