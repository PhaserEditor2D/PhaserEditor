
namespace phasereditor2d.ui.ide.views.files {

    import viewers = colibri.ui.controls.viewers;
    import io = colibri.core.io;


    export class FileLabelProvider implements viewers.ILabelProvider {
        getLabel(obj: io.FilePath): string {
            return obj.getName();
        }
    }
}