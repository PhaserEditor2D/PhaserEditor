/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileCellRendererProvider implements viewers.ICellRendererProvider {
        
        getCellRenderer(file: io.FilePath): viewers.ICellRenderer {

            if (Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file) === ui.ide.CONTENT_TYPE_IMAGE) {
                return new FileImageRenderer();
            }

            return new FileCellRenderer();
        }

        preload(file: io.FilePath) : Promise<controls.PreloadResult> {
            return Workbench.getWorkbench().getContentTypeRegistry().preload(file);
        }
    }
}