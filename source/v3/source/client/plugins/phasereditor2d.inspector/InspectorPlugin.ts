namespace phasereditor2d.inspector {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_INSPECTOR = "inspector";

    export class InspectorPlugin extends ide.Plugin {

        private static _instance = new InspectorPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.inspector.InspectorPlugin");
        }

        async preloadIcons() {
            await this.getIcon(ICON_INSPECTOR).preload();
        }

        registerCSSUrls(urls: string[]) {
            urls.push("plugins/phasereditor2d.inspector/ui/css/InspectorView.css");
        }

        getIcon(name: string) {
            return controls.Controls.getIcon(name, "plugins/phasereditor2d.inspector/ui/icons");
        }

    }

}