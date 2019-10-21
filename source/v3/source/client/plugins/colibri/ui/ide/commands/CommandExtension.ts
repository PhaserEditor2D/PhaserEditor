namespace colibri.ui.ide.commands {

    export class CommandExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.commands";

        private _configurer: (manager: CommandManager) => void;

        constructor(id: string, configurer: (manager: CommandManager) => void) {
            super(id);

            this._configurer = configurer;
        }

        getConfigurer() {
            return this._configurer;
        }

    }

}