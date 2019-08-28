namespace phasereditor2d.ui.controls {
    export interface IToolbar {
        addAction(action: Action);

        getActions(): Action[];
    }
}