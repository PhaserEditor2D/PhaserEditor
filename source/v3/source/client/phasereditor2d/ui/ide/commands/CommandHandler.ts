namespace phasereditor2d.ui.ide.commands {

    export class CommandHandler {

        private _id : string;
        private _testFunc: (args: CommandArgs) => boolean;
        private _executeFunc: (args: CommandArgs) => void;

        constructor(config: {
            id: string,
            testFunc?: (args: CommandArgs) => boolean,
            executeFunc?: (args: CommandArgs) => void
        }) {

            this._id = config.id;
            this._testFunc = config.testFunc;
            this._executeFunc = config.executeFunc;

        }

        getId() {
            return this._id;
        }

        test(args: CommandArgs) {
            return this._testFunc ? this._testFunc(args) : true;
        }

        execute(args: CommandArgs): void {

            if (this._executeFunc) {
                this._executeFunc(args);
            }

        }
    }

}