/// <reference path="./ui/ide/Plugin.ts" />
/// <reference path="./ui/ide/Workbench.ts" />

namespace colibri {

    export class ColibriPlugin extends ui.ide.Plugin {

        private static _instance;

        static getInstance() {
            return this._instance ?? (this._instance = new ColibriPlugin());
        }

        private _openingProject: boolean;

        private constructor() {
            super("colibri");

            this._openingProject = false;
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // themes

            reg.addExtension(
                new colibri.ui.ide.themes.ThemeExtension(colibri.ui.controls.Controls.LIGHT_THEME),
                new colibri.ui.ide.themes.ThemeExtension(colibri.ui.controls.Controls.DARK_THEME)
            );
        }

    }
}