namespace phasereditor2d.phaser {

    import ide = colibri.ui.ide;

    export class PhaserPlugin extends ide.Plugin {

        private static _instance = new PhaserPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.phaser");
        }

    }

}