var phasereditor2d;
(function (phasereditor2d) {
    var phaser;
    (function (phaser) {
        var ide = colibri.ui.ide;
        class PhaserPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.phaser");
            }
            static getInstance() {
                return this._instance;
            }
        }
        PhaserPlugin._instance = new PhaserPlugin();
        phaser.PhaserPlugin = PhaserPlugin;
    })(phaser = phasereditor2d.phaser || (phasereditor2d.phaser = {}));
})(phasereditor2d || (phasereditor2d = {}));
