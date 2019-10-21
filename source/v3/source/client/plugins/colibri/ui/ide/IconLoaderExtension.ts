namespace colibri.ui.ide {

    export class IconLoaderExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.IconLoaderExtension";

        static withPluginFiles(plugin: ide.Plugin, iconNames: string[]) {

            const id = `${plugin.getId()}.IconLoaderExtension`;

            const icons = iconNames.map(name => plugin.getIcon(name));

            return new IconLoaderExtension(id, icons);
        }

        private _icons: controls.IImage[];

        constructor(id: string, icons: controls.IImage[]) {
            super(id);

            this._icons = icons;
        }

        getIcons() {
            return this._icons;
        }

    }

}