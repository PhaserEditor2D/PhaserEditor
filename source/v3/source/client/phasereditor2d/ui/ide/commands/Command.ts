namespace phasereditor2d.ui.ide.commands {

    export class Command {

        private _id: string;

        constructor(id: string) {
            this._id = id;
        }

        getId() {
            return this._id;
        }
    }

}