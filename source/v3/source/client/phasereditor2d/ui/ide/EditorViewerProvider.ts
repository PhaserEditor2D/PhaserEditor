namespace phasereditor2d.ui.ide {

    import viewers = controls.viewers;

    export abstract class EditorViewerProvider {

        abstract getContentProvider() : viewers.ITreeContentProvider;

        abstract getLabelProvider() : viewers.ILabelProvider;

        abstract getCellRendererProvider() : viewers.ICellRendererProvider;

        abstract getTreeViewerRenderer(viewer : controls.viewers.TreeViewer) : viewers.TreeViewerRenderer;

        abstract getPropertySectionProvider() : controls.properties.PropertySectionProvider;

        abstract getInput() : any;

        abstract preload() : Promise<void>;
    }
}