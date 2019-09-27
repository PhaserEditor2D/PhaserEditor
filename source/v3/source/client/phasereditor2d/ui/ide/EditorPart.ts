namespace phasereditor2d.ui.ide {

    export abstract class EditorPart extends Part {

        private _input : any;

        constructor(id: string) {
            super(id);
            this.addClass("EditorPart");
        }

        getInput() {
            return this._input;
        }

        setInput(input : any) : void {
            this._input = input;
        }

        getBlocksProvider() : EditorBlocksProvider {
            return null;
        }

    }
}