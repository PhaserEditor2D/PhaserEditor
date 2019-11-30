namespace colibri.ui.ide.commands {

    export class CommandExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.commands";

        private _configurer: (manager: CommandManager) => void;

        constructor(configurer: (manager: CommandManager) => void) {
            super();

            this._configurer = configurer;
        }

        getConfigurer() {
            return this._configurer;
        }

    }

}