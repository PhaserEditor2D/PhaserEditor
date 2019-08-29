/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileCellRendererProvider implements viewers.ICellRendererProvider {
        getCellRenderer(element: io.FilePath): viewers.ICellRenderer {
            if (element.getContentType() === "img") {
                return new FileImageRenderer(false);
            }
            return new FileCellRenderer();
        }
    }
}