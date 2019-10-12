namespace colibri.ui.controls {
    export interface IToolbar {
        addAction(action: Action);

        getActions(): Action[];
    }
}