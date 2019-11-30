namespace colibri.ui.ide {

    export class IconLoaderExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.IconLoaderExtension";

        static withPluginFiles(plugin: ide.Plugin, iconNames: string[]) {

            const icons = iconNames.map(name => plugin.getIcon(name));

            return new IconLoaderExtension(icons);
        }

        private _icons: controls.IImage[];

        constructor(icons: controls.IImage[]) {
            super();

            this._icons = icons;
        }

        getIcons() {
            return this._icons;
        }

    }

}