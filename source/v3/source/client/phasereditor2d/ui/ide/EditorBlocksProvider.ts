namespace phasereditor2d.ui.ide {

    import viewers = controls.viewers;

    export abstract class EditorBlocksProvider {

        abstract getContentProvider() : viewers.ITreeContentProvider;

        abstract getLabelProvider() : viewers.ILabelProvider;

        abstract getCellRendererProvider() : viewers.ICellRendererProvider;

        abstract getInput() : any;

        abstract preload() : Promise<void>;
    }
}