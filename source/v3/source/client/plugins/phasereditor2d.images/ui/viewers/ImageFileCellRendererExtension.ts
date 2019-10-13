namespace phasereditor2d.images.ui.viewers {

    import controls = colibri.ui.controls;

    class Provider implements controls.viewers.ICellRendererProvider {
        
        getCellRenderer(element: any): colibri.ui.controls.viewers.ICellRenderer {
            return new ImageFileCellRenderer();
        }        
        
        preload(element: any): Promise<colibri.ui.controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

    export class ImageFileCellRendererExtension extends files.ui.viewers.ContentTypeCellRendererExtension {
        
        constructor() {
            super("phasereditor2d.images.ui.viewers.ImageFileCellRendererExtension");
        }

        getRendererProvider(contentType: string): colibri.ui.controls.viewers.ICellRendererProvider {
            
            if (contentType === files.core.CONTENT_TYPE_IMAGE) {
                return new Provider();
            }

            return null;
        }

    }

}