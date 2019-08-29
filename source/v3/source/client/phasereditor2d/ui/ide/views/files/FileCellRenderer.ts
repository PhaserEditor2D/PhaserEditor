/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileCellRenderer extends viewers.LabelCellRenderer {

        getImage(obj: any): controls.IIcon {
            const file = <io.FilePath>obj;
            if (file.isFile()) {
                const type = file.getContentType();
                const icon = Workbench.getWorkbench().getContentTypeIcon(type);
                if (icon) {
                    return icon;
                }
            } else {
                return controls.Controls.getIcon(controls.Controls.ICON_FOLDER);
            }

            return controls.Controls.getIcon(controls.Controls.ICON_FILE);
        }
    }
}