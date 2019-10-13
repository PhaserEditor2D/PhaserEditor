namespace phasereditor2d.scene.ui.viewers {

    import controls = colibri.ui.controls;

    class Provider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {
            return new SceneFileCellRenderer();
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

    export class SceneFileCellRendererExtension extends files.ui.viewers.ContentTypeCellRendererExtension {

        constructor() {
            super("phasereditor2d.scene.ui.viewers.SceneFileCellRendererExtension");
        }

        getRendererProvider(contentType: string): colibri.ui.controls.viewers.ICellRendererProvider {

            if (contentType === scene.core.CONTENT_TYPE_SCENE) {
                return new Provider();
            }

            return null;
        }

    }

}