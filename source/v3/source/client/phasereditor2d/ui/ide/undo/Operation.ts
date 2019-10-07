namespace phasereditor2d.ui.ide.undo {

    export abstract class Operation {

        abstract undo() : void;

        abstract redo() : void;

    }

}