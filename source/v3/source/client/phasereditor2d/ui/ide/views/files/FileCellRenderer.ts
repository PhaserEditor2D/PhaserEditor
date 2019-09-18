/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.views.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileCellRenderer extends viewers.LabelCellRenderer {

        getImage(obj: any): controls.IImage {
            const file = <io.FilePath>obj;
            if (file.isFile()) {
                const ct = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                const icon = Workbench.getWorkbench().getContentTypeIcon(ct);
                if (icon) {
                    return icon;
                }
            } else {
                return controls.Controls.getIcon(ICON_FOLDER);
            }

            return controls.Controls.getIcon(ICON_FILE);
        }

        preload(obj: any) {
            const file = <io.FilePath>obj;
            if (file.isFile()) {
                return Workbench.getWorkbench().getContentTypeRegistry().preload(file);
            }
            return super.preload(obj);
        }
    }
}